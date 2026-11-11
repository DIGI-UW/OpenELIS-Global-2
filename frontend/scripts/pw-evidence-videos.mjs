// pw-evidence-videos — one MP4 per recorded evidence story, filed by lane and stage.
//
// Reads the JSON report of a `harness-demo-video` run. Every test it recorded
// must carry `@evidence`, one lane tag (@astm, @hl7, @file) and one stage tag
// (@admin, @setup, @results, @exceptions), and must have passed with a video;
// anything else stops the run and is listed. Each video is re-encoded to H.264
// (faststart, so GitHub and Slack play it inline) with every stretch in which
// nothing on screen changes cut down to a few seconds, long enough to read the
// caption over it, and written to <out>/<lane>/<stage>/<nn>-<story>.mp4.
// <out>/index.json lists what was written. Nothing here is committed.
//
// Usage:
//   node scripts/pw-evidence-videos.mjs --report test-results/harness-demo-video.json --out evidence-videos
//
// Requires ffmpeg and ffprobe on PATH.

import { execFileSync, spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import fs from "node:fs";
import path from "node:path";

const LANES = ["@astm", "@hl7", "@file"];
const STAGES = ["@admin", "@setup", "@results", "@exceptions"];
const PROJECT = "harness-demo-video";
// Seconds of every still stretch that stay, so the caption over it can be read.
const KEEP_STILL_SECONDS = 3.5;

function parseArgs(argv) {
  const out = {
    report: "test-results/harness-demo-video.json",
    out: "evidence-videos",
  };
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === "--report") out.report = argv[++i];
    else if (arg === "--out") out.out = argv[++i];
    else if (arg === "-h" || arg === "--help") {
      console.log(
        "pw-evidence-videos --report <playwright json report> --out <dir>",
      );
      process.exit(0);
    } else {
      console.error(`unknown argument: ${arg}`);
      process.exit(2);
    }
  }
  return out;
}

/** Every test the video project recorded, with its tags, status and video. */
function recordedStories(report) {
  const stories = [];
  const visit = (suite, file) => {
    for (const spec of suite.specs ?? []) {
      for (const test of spec.tests ?? []) {
        if (test.projectName !== PROJECT) continue;
        const result = test.results[test.results.length - 1];
        const video = result?.attachments?.find((a) => a.name === "video");
        stories.push({
          title: spec.title,
          file: spec.file ?? file,
          line: spec.line ?? 0,
          // The JSON reporter writes tags without their "@".
          tags: (spec.tags ?? test.tags ?? []).map((tag) =>
            tag.startsWith("@") ? tag : `@${tag}`,
          ),
          status: result?.status ?? "missing",
          video: video?.path ?? null,
        });
      }
    }
    for (const child of suite.suites ?? []) visit(child, suite.file ?? file);
  };
  for (const suite of report.suites ?? []) visit(suite, suite.file);
  return stories;
}

function problemsWith(story) {
  const problems = [];
  if (!story.tags.includes("@evidence")) problems.push("not tagged @evidence");
  const lanes = story.tags.filter((t) => LANES.includes(t));
  const stages = story.tags.filter((t) => STAGES.includes(t));
  if (lanes.length !== 1)
    problems.push(`lane tags: ${lanes.join(" ") || "none"}`);
  if (stages.length !== 1)
    problems.push(`stage tags: ${stages.join(" ") || "none"}`);
  if (story.status !== "passed") problems.push(`status ${story.status}`);
  if (!story.video) problems.push("no video");
  return problems;
}

function ffprobeSeconds(file) {
  const out = execFileSync(
    "ffprobe",
    ["-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", file],
    { encoding: "utf8" },
  );
  return Number(out.trim());
}

/** Intervals in which no frame changes; Playwright repeats the last frame while nothing paints. */
function stillStretches(video) {
  const run = spawnSync(
    "ffmpeg",
    [
      "-hide_banner",
      "-i",
      video,
      "-vf",
      "fps=25,freezedetect=n=-42dB:d=4.5",
      "-map",
      "0:v",
      "-f",
      "null",
      "-",
    ],
    { encoding: "utf8" },
  );
  if (run.status !== 0) throw new Error(run.stderr);
  const stretches = [];
  let begin = null;
  for (const line of run.stderr.split("\n")) {
    if (line.includes("freeze_start:")) {
      begin = Number(line.slice(line.lastIndexOf(":") + 1));
    } else if (line.includes("freeze_end:") && begin !== null) {
      stretches.push([begin, Number(line.slice(line.lastIndexOf(":") + 1))]);
      begin = null;
    }
  }
  if (begin !== null) stretches.push([begin, ffprobeSeconds(video)]);
  return stretches;
}

/** Encodes one story, cutting each still stretch down to KEEP_STILL_SECONDS; returns seconds cut. */
function encodeStory(video, target) {
  const cuts = stillStretches(video)
    .map(([a, b]) => [a + KEEP_STILL_SECONDS, b - 0.2])
    .filter(([a, b]) => b - a > 0.5);
  let chain = "fps=25";
  if (cuts.length > 0) {
    const keep = cuts
      .map(([a, b]) => `between(t,${a.toFixed(2)},${b.toFixed(2)})`)
      .join("+");
    chain += `,select='not(${keep})',setpts=N/25/TB`;
  }
  execFileSync("ffmpeg", [
    "-y",
    "-loglevel",
    "error",
    "-i",
    video,
    "-vf",
    chain,
    "-c:v",
    "libx264",
    "-preset",
    "slow",
    "-crf",
    "22",
    "-pix_fmt",
    "yuv420p",
    "-movflags",
    "+faststart",
    "-an",
    target,
  ]);
  return cuts.reduce((sum, [a, b]) => sum + (b - a), 0);
}

function sha256(file) {
  return createHash("sha256").update(fs.readFileSync(file)).digest("hex");
}

function slug(title) {
  return title
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "")
    .slice(0, 60)
    .replace(/-[^-]*$/, (tail) => (tail.length > 12 ? "" : tail));
}

function main() {
  const args = parseArgs(process.argv.slice(2));
  const report = JSON.parse(fs.readFileSync(args.report, "utf8"));
  const stories = recordedStories(report).sort(
    (a, b) => a.file.localeCompare(b.file) || a.line - b.line,
  );
  if (stories.length === 0) {
    console.error(`no ${PROJECT} tests in ${args.report}`);
    process.exit(1);
  }
  const problems = stories
    .map((story) => ({ story, problems: problemsWith(story) }))
    .filter(({ problems }) => problems.length > 0);
  if (problems.length > 0) {
    for (const { story, problems: list } of problems) {
      console.error(`${story.title}\n  ${list.join("; ")}`);
    }
    process.exit(1);
  }

  fs.rmSync(args.out, { recursive: true, force: true });
  const counters = new Map();
  const index = [];
  for (const story of stories) {
    const lane = story.tags.find((t) => LANES.includes(t)).slice(1);
    const stage = story.tags.find((t) => STAGES.includes(t)).slice(1);
    const dir = path.join(args.out, lane, stage);
    fs.mkdirSync(dir, { recursive: true });
    const n = (counters.get(dir) ?? 0) + 1;
    counters.set(dir, n);
    const target = path.join(
      dir,
      `${String(n).padStart(2, "0")}-${slug(story.title)}.mp4`,
    );
    const cut = encodeStory(story.video, target);
    const seconds = ffprobeSeconds(target);
    index.push({
      lane,
      stage,
      title: story.title,
      spec: story.file,
      file: path.relative(args.out, target),
      seconds: Math.round(seconds * 10) / 10,
      stillSecondsCut: Math.round(cut * 10) / 10,
      bytes: fs.statSync(target).size,
      sha256: sha256(target),
    });
    console.log(
      `${lane}/${stage}: ${path.basename(target)} ${seconds.toFixed(0)}s (${cut.toFixed(0)}s of still screen cut)`,
    );
  }
  fs.writeFileSync(
    path.join(args.out, "index.json"),
    JSON.stringify(index, null, 2) + "\n",
  );
  console.log(`${index.length} videos under ${args.out}`);
}

main();
