/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.document.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * In-memory implementation of RateLimitService using a sliding window approach.
 * Tracks upload timestamps per patient and enforces rate limits.
 * 
 * Note: This implementation is suitable for single-instance deployments.
 * For distributed deployments, consider using Redis or similar distributed cache.
 */
@Service
public class RateLimitServiceImpl implements RateLimitService {

    @Value("${document.rate-limit.max-uploads:5}")
    private int maxUploads;

    @Value("${document.rate-limit.window-seconds:60}")
    private long windowSeconds;

    // Map of patientId -> list of upload timestamps (within the window)
    private final Map<String, java.util.List<Instant>> uploadHistory = new ConcurrentHashMap<>();

    @Override
    public synchronized boolean isAllowed(String patientId) {
        cleanupOldEntries(patientId);
        java.util.List<Instant> uploads = uploadHistory.getOrDefault(patientId, new java.util.ArrayList<>());
        return uploads.size() < maxUploads;
    }

    @Override
    public synchronized void recordUpload(String patientId) {
        Instant now = Instant.now();
        java.util.List<Instant> uploads = uploadHistory.computeIfAbsent(patientId, k -> new java.util.ArrayList<>());
        uploads.add(now);
        cleanupOldEntries(patientId);
    }

    @Override
    public synchronized long getTimeUntilNextAllowed(String patientId) {
        cleanupOldEntries(patientId);
        java.util.List<Instant> uploads = uploadHistory.getOrDefault(patientId, new java.util.ArrayList<>());
        
        if (uploads.size() < maxUploads) {
            return 0;
        }

        // Find the oldest upload in the window
        Instant oldest = uploads.get(0);
        Instant windowEnd = oldest.plusSeconds(windowSeconds);
        Instant now = Instant.now();

        if (windowEnd.isAfter(now)) {
            return windowEnd.getEpochSecond() - now.getEpochSecond();
        }

        return 0;
    }

    private void cleanupOldEntries(String patientId) {
        Instant cutoff = Instant.now().minusSeconds(windowSeconds);
        java.util.List<Instant> uploads = uploadHistory.get(patientId);
        if (uploads != null) {
            uploads.removeIf(timestamp -> timestamp.isBefore(cutoff));
            if (uploads.isEmpty()) {
                uploadHistory.remove(patientId);
            }
        }
    }
}

