
Running 14 tests using 1 worker
(node:36916) Warning: The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.
(Use `node --trace-warnings ...` to show where the warning was created)
(node:36916) Warning: The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.
(Use `node --trace-warnings ...` to show where the warning was created)
×(node:36991) Warning: The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.
(Use `node --trace-warnings ...` to show where the warning was created)
(node:36991) Warning: The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.
(Use `node --trace-warnings ...` to show where the warning was created)
×(node:37001) Warning: The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.
(Use `node --trace-warnings ...` to show where the warning was created)
(node:37001) Warning: The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.
(Use `node --trace-warnings ...` to show where the warning was created)
F

  1) [setup] › playwright/tests/auth.setup.ts:5:6 › authenticate ───────────────────────────────────

    Error: page.goto: net::ERR_CONNECTION_REFUSED at https://localhost/
    Call log:
    [2m  - navigating to "https://localhost/", waiting until "load"[22m


       7 |   const password = process.env.TEST_PASS || "adminADMIN!";
       8 |
    >  9 |   await page.goto("/");
         |              ^
      10 |   await page.getByLabel("Username").fill(username);
      11 |   await page.getByLabel("Password").fill(password);
      12 |   await page.getByRole("button", { name: "Login" }).click();
        at /Users/pmanko/code/OpenELIS-Global-2/frontend/playwright/tests/auth.setup.ts:9:14

    attachment #1: screenshot (image/png) ──────────────────────────────────────────────────────────
    test-results/auth.setup.ts-authenticate-setup/test-failed-1.png
    ────────────────────────────────────────────────────────────────────────────────────────────────

    Retry #1 ───────────────────────────────────────────────────────────────────────────────────────

    Error: page.goto: net::ERR_CONNECTION_REFUSED at https://localhost/
    Call log:
    [2m  - navigating to "https://localhost/", waiting until "load"[22m


       7 |   const password = process.env.TEST_PASS || "adminADMIN!";
       8 |
    >  9 |   await page.goto("/");
         |              ^
      10 |   await page.getByLabel("Username").fill(username);
      11 |   await page.getByLabel("Password").fill(password);
      12 |   await page.getByRole("button", { name: "Login" }).click();
        at /Users/pmanko/code/OpenELIS-Global-2/frontend/playwright/tests/auth.setup.ts:9:14

    attachment #1: screenshot (image/png) ──────────────────────────────────────────────────────────
    test-results/auth.setup.ts-authenticate-setup-retry1/test-failed-1.png
    ────────────────────────────────────────────────────────────────────────────────────────────────

    attachment #2: trace (application/zip) ─────────────────────────────────────────────────────────
    test-results/auth.setup.ts-authenticate-setup-retry1/trace.zip
    Usage:

        npx playwright show-trace test-results/auth.setup.ts-authenticate-setup-retry1/trace.zip

    ────────────────────────────────────────────────────────────────────────────────────────────────

    Retry #2 ───────────────────────────────────────────────────────────────────────────────────────

    Error: page.goto: net::ERR_CONNECTION_REFUSED at https://localhost/
    Call log:
    [2m  - navigating to "https://localhost/", waiting until "load"[22m


       7 |   const password = process.env.TEST_PASS || "adminADMIN!";
       8 |
    >  9 |   await page.goto("/");
         |              ^
      10 |   await page.getByLabel("Username").fill(username);
      11 |   await page.getByLabel("Password").fill(password);
      12 |   await page.getByRole("button", { name: "Login" }).click();
        at /Users/pmanko/code/OpenELIS-Global-2/frontend/playwright/tests/auth.setup.ts:9:14

    attachment #1: screenshot (image/png) ──────────────────────────────────────────────────────────
    test-results/auth.setup.ts-authenticate-setup-retry2/test-failed-1.png
    ────────────────────────────────────────────────────────────────────────────────────────────────

  1 failed
    [setup] › playwright/tests/auth.setup.ts:5:6 › authenticate ────────────────────────────────────
  13 did not run
::error file=playwright/tests/auth.setup.ts,title=[setup] › playwright/tests/auth.setup.ts:5:6 › authenticate,line=9,col=14::  1) [setup] › playwright/tests/auth.setup.ts:5:6 › authenticate ───────────────────────────────────%0A    Error: page.goto: net::ERR_CONNECTION_REFUSED at https://localhost/%0A    Call log:%0A      - navigating to "https://localhost/", waiting until "load"%0A%0A%0A       7 |   const password = process.env.TEST_PASS || "adminADMIN!";%0A       8 |%0A    >  9 |   await page.goto("/");%0A         |              ^%0A      10 |   await page.getByLabel("Username").fill(username);%0A      11 |   await page.getByLabel("Password").fill(password);%0A      12 |   await page.getByRole("button", { name: "Login" }).click();%0A        at /Users/pmanko/code/OpenELIS-Global-2/frontend/playwright/tests/auth.setup.ts:9:14
::error file=playwright/tests/auth.setup.ts,title=[setup] › playwright/tests/auth.setup.ts:5:6 › authenticate,line=9,col=14::  1) [setup] › playwright/tests/auth.setup.ts:5:6 › authenticate ───────────────────────────────────%0A%0A    Retry #1 ───────────────────────────────────────────────────────────────────────────────────────%0A    Error: page.goto: net::ERR_CONNECTION_REFUSED at https://localhost/%0A    Call log:%0A      - navigating to "https://localhost/", waiting until "load"%0A%0A%0A       7 |   const password = process.env.TEST_PASS || "adminADMIN!";%0A       8 |%0A    >  9 |   await page.goto("/");%0A         |              ^%0A      10 |   await page.getByLabel("Username").fill(username);%0A      11 |   await page.getByLabel("Password").fill(password);%0A      12 |   await page.getByRole("button", { name: "Login" }).click();%0A        at /Users/pmanko/code/OpenELIS-Global-2/frontend/playwright/tests/auth.setup.ts:9:14
::error file=playwright/tests/auth.setup.ts,title=[setup] › playwright/tests/auth.setup.ts:5:6 › authenticate,line=9,col=14::  1) [setup] › playwright/tests/auth.setup.ts:5:6 › authenticate ───────────────────────────────────%0A%0A    Retry #2 ───────────────────────────────────────────────────────────────────────────────────────%0A    Error: page.goto: net::ERR_CONNECTION_REFUSED at https://localhost/%0A    Call log:%0A      - navigating to "https://localhost/", waiting until "load"%0A%0A%0A       7 |   const password = process.env.TEST_PASS || "adminADMIN!";%0A       8 |%0A    >  9 |   await page.goto("/");%0A         |              ^%0A      10 |   await page.getByLabel("Username").fill(username);%0A      11 |   await page.getByLabel("Password").fill(password);%0A      12 |   await page.getByRole("button", { name: "Login" }).click();%0A        at /Users/pmanko/code/OpenELIS-Global-2/frontend/playwright/tests/auth.setup.ts:9:14
::notice title=🎭 Playwright Run Summary::  1 failed%0A    [setup] › playwright/tests/auth.setup.ts:5:6 › authenticate ────────────────────────────────────%0A  13 did not run
