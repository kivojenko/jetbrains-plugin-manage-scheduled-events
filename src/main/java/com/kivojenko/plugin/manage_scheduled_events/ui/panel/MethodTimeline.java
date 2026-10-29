package com.kivojenko.plugin.manage_scheduled_events.ui.panel;

import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** A 24-hour strip starting now, with a bar for each run; runs at most 5 minutes apart are merged into one bar. */
public class MethodTimeline extends JComponent {
    private static final Color BAR_COLOR = new JBColor(new Color(0x4A90E2), new Color(0x4A90E2));

    private List<ZonedDateTime> occurrences = List.of();
    private List<TimeCluster> clusters = List.of();
    private float width = getWidth();
    private float hourWidth;
    private float minuteWidth;

    public MethodTimeline() {
        hourWidth = width / 24;
        minuteWidth = hourWidth / 60;
    }

    public void setOccurrences(List<ZonedDateTime> occurrences) {
        this.occurrences = occurrences;
        this.clusters = mergeOccurrences(occurrences);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(700, 18);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (occurrences.isEmpty()) return;

        width = getWidth();
        hourWidth = width / 24;
        minuteWidth = hourWidth / 60;

        var g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int height = getHeight();
        var now = ZonedDateTime.now();

        for (var cluster : clusters) {
            float startX = getX(cluster.start, now);
            float endX = getX(cluster.end, now);
            float barW = Math.max(3, endX - startX + 1);
            int barY = 2;
            int barH = height - 4;

            cluster.bounds = new Rectangle((int) startX, barY, (int) barW, barH);
            g2.setColor(BAR_COLOR);
            g2.fillRoundRect((int) startX, barY, (int) barW, barH, 4, 4);
        }

        g2.dispose();
    }

    @Override
    public String getToolTipText() {
        return clusters.isEmpty() ? null : "Runs at " + clusters.getFirst().start.toLocalDateTime();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        for (var cluster : clusters) {
            if (cluster.bounds != null && cluster.bounds.contains(e.getPoint())) {
                var start = cluster.start.toLocalTime().toString();
                var end = cluster.end.toLocalTime().toString();
                return start.equals(end) ? "Runs at " + start : "Runs often from " + start + " to " + end;
            }
        }
        return null;
    }

    private float getX(ZonedDateTime time, ZonedDateTime now) {
        int additionalHours = now.getDayOfMonth() != time.getDayOfMonth() ? 24 : 0;
        int hours = time.getHour() - now.getHour() + additionalHours;
        return hours * hourWidth + time.getMinute() * minuteWidth;
    }

    private List<TimeCluster> mergeOccurrences(List<ZonedDateTime> occurrences) {
        if (occurrences.isEmpty()) return List.of();

        List<TimeCluster> clusters = new ArrayList<>();
        var start = occurrences.getFirst();
        var prev = start;

        for (var current : occurrences) {
            if (Duration.between(prev, current).toMinutes() > 5) {
                clusters.add(new TimeCluster(start, prev));
                start = current;
            }
            prev = current;
        }

        clusters.add(new TimeCluster(start, prev));
        return clusters;
    }

    private static class TimeCluster {
        ZonedDateTime start;
        ZonedDateTime end;
        Rectangle bounds;

        TimeCluster(ZonedDateTime start, ZonedDateTime end) {
            this.start = start;
            this.end = end;
        }
    }
}
