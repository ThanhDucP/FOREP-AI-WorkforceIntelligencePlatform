package com.aiworkforce.task.service;

import com.aiworkforce.core.enums.IntegrationProvider;
import com.aiworkforce.core.enums.TaskPriority;
import com.aiworkforce.core.enums.TaskStatus;
import com.aiworkforce.task.entity.Task;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class TaskAssessmentService {

    public void assess(Task task, String source) {
        assess(task, source, null);
    }

    public void assess(Task task, String source, GithubCommitMetrics githubMetrics) {
        int difficulty = firstPositive(task.getDifficultyScore(), estimateDifficulty(task));
        int progress = clamp(firstNonNull(task.getProgressPercent(), estimateProgress(task)), 0, 100);
        double leadScore = estimateLeadScore(task.getLeadEvaluation());
        double hourScore = scoreEstimatedHours(task.getEstimatedHours());

        task.setDifficultyScore(difficulty);
        task.setProgressPercent(progress);
        task.setAssessmentSource(source);
        task.setAssessedAt(LocalDateTime.now());

        double githubScore = 0.0;
        if (githubMetrics != null) {
            task.setGithubCommitCount(githubMetrics.commitCount());
            task.setGithubCommitDifficultyScore(githubMetrics.difficultyScore());
            task.setGithubCommitSizeScore(githubMetrics.sizeScore());
            task.setGithubCommitScore(githubMetrics.score());
            githubScore = githubMetrics.score();
        } else if (task.getGithubCommitScore() != null) {
            githubScore = task.getGithubCommitScore();
        }

        double taskScore = (difficulty * 5.0)
                + (hourScore * 1.5)
                + (progress * 0.2)
                + (leadScore * 1.5)
                + githubScore;
        task.setTaskScore(roundOneDecimal(clampDouble(taskScore, 0.0, 100.0)));

        task.setAssessmentSummary(buildSummary(task, difficulty, progress, leadScore));
    }

    public GithubCommitMetrics assessGithubCommits(int commitCount, int additions, int deletions, int changedFiles) {
        int churn = Math.max(0, additions) + Math.max(0, deletions);
        int difficulty = clamp(1 + (changedFiles / 4) + (churn / 250), 1, 10);
        int size = clamp(1 + (commitCount / 2) + (churn / 150), 1, 10);
        double score = roundOneDecimal((commitCount * 1.5) + (difficulty * 2.0) + (size * 1.5));
        return new GithubCommitMetrics(commitCount, additions, deletions, changedFiles, difficulty, size, score);
    }

    private int estimateDifficulty(Task task) {
        int score = 3;
        TaskPriority priority = task.getPriority();
        if (priority == TaskPriority.CRITICAL) score += 3;
        else if (priority == TaskPriority.HIGH) score += 2;
        else if (priority == TaskPriority.MEDIUM) score += 1;

        score += Math.min(3, Math.max(0, task.getEstimatedHours()) / 8);
        if (task.getStoryPoints() != null) score += Math.min(3, task.getStoryPoints() / 3);
        if (Boolean.TRUE.equals(task.getIsOnCriticalPath())) score += 1;
        if (task.getDueDate() != null && LocalDateTime.now().until(task.getDueDate(), ChronoUnit.DAYS) <= 2) score += 1;
        if (task.getSourceProvider() != null && task.getSourceProvider() != IntegrationProvider.INTERNAL) score += 1;
        return clamp(score, 1, 10);
    }

    private int estimateProgress(Task task) {
        TaskStatus status = task.getStatus();
        if (status == TaskStatus.DONE) return 100;
        if (status == TaskStatus.REVIEW) return 80;
        if (status == TaskStatus.IN_PROGRESS) return 50;
        if (status == TaskStatus.OVERDUE) return 35;
        return 0;
    }

    private double estimateLeadScore(String leadEvaluation) {
        if (leadEvaluation == null || leadEvaluation.isBlank()) return 0.0;
        String text = leadEvaluation.toLowerCase();
        if (text.contains("excellent") || text.contains("great") || text.contains("tot") || text.contains("xuất sắc")) return 10.0;
        if (text.contains("risk") || text.contains("block") || text.contains("delay") || text.contains("chậm")) return 4.0;
        return 7.0;
    }

    private double scoreEstimatedHours(int hours) {
        if (hours <= 0) return 1.0;
        return clampDouble(hours / 4.0, 1.0, 10.0);
    }

    private String buildSummary(Task task, int difficulty, int progress, double leadScore) {
        String assignee = "Unassigned";
        if (task.getAssignee() != null) {
            assignee = ((task.getAssignee().getFirstName() != null ? task.getAssignee().getFirstName() : "") + " "
                    + (task.getAssignee().getLastName() != null ? task.getAssignee().getLastName() : "")).trim();
            if (assignee.isBlank()) {
                assignee = "Assigned employee";
            }
        }
        String deadline = task.getDueDate() != null ? task.getDueDate().toLocalDate().toString() : "No deadline";
        int commitCount = firstNonNull(task.getGithubCommitCount(), 0);
        return String.format(
                "Task '%s' assigned to %s, deadline %s. Difficulty %d/10, estimated %d hours, progress %d%%, lead score %.1f, GitHub commits %d, total score %.1f.",
                task.getTitle(),
                assignee,
                deadline,
                difficulty,
                task.getEstimatedHours(),
                progress,
                leadScore,
                commitCount,
                task.getTaskScore()
        );
    }

    private int firstPositive(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private int firstNonNull(Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record GithubCommitMetrics(
            int commitCount,
            int additions,
            int deletions,
            int changedFiles,
            int difficultyScore,
            int sizeScore,
            double score
    ) {
    }
}
