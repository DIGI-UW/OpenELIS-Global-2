package org.openelisglobal.pathology;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.openelisglobal.program.service.PathologyReportRenderer;
import org.openelisglobal.program.service.PathologyReportService;
import org.openelisglobal.program.service.ReportPaths;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Stands in for the report template while a test runs: it answers a five-byte
 * PDF body, records the amended flag each render was given, and can be told to
 * fail or to wait.
 */
public class StubReportRenderer implements PathologyReportRenderer {

    /** What every render answers: the PDF signature and nothing else. */
    public static final byte[] BODY = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    /** A placeholder: the stub never reads the paths. */
    public static final ReportPaths PATHS = new ReportPaths("reports/", "images/");

    private static final String FIELD = "pathologyReportRenderer";

    private final Object service;
    private final Object original;

    /** The amended flag of each render, in order. */
    public final List<Boolean> amendedSeen = new CopyOnWriteArrayList<>();

    /** Counted down when a render starts. */
    public final CountDownLatch rendering = new CountDownLatch(1);

    /** When set, a render waits for it before answering. */
    public volatile CountDownLatch release;

    /** When set, run inside the render, in the caller's transaction. */
    public volatile Runnable during;

    /** When set, a render fails as a template failure would. */
    public volatile boolean fail;

    private StubReportRenderer(Object service, Object original) {
        this.service = service;
        this.original = original;
    }

    /** Puts a stub in the report service's place for its renderer. */
    public static StubReportRenderer installIn(PathologyReportService reportService) {
        Object target = AopTestUtils.getUltimateTargetObject(reportService);
        StubReportRenderer stub = new StubReportRenderer(target, ReflectionTestUtils.getField(target, FIELD));
        ReflectionTestUtils.setField(target, FIELD, stub);
        return stub;
    }

    /** Gives the service its own renderer back. */
    public void restore() {
        ReflectionTestUtils.setField(service, FIELD, original);
    }

    @Override
    public byte[] render(Integer pathologySampleId, String sysUserId, ReportPaths paths, boolean amended) {
        amendedSeen.add(amended);
        rendering.countDown();
        CountDownLatch waitFor = release;
        if (waitFor != null) {
            try {
                if (!waitFor.await(60, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("the test never let the render finish");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("the render was interrupted", e);
            }
        }
        if (during != null) {
            during.run();
        }
        if (fail) {
            throw new IllegalStateException("the report template failed");
        }
        return BODY.clone();
    }
}
