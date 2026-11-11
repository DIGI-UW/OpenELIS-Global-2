package org.openelisglobal.resultrun.form;

/**
 * Where a run's row stands for review (OGC-1200 FR-E1a), in the order the run
 * view lists them: exceptions and held rows first, then critical, abnormal and
 * normal values. Controls are listed last and only under All.
 */
public enum ResultRunRowCategory {
    EXCEPTION, HELD, CRITICAL, ABNORMAL, NORMAL, CONTROL
}
