import { createRequire } from 'node:module';
import { mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
const require = createRequire(resolve('frontend/package.json'));
const { chromium, expect } = require('@playwright/test');
require('dotenv').config({path: '.env', quiet: true});
const evidence = resolve('.devin/artifacts/m5/electronic-reflex/browser-final');
await mkdir(evidence, { recursive: true });
const browser = await chromium.launch({headless: true});
const context = await browser.newContext({baseURL:'https://localhost:33880',ignoreHTTPSErrors:true,viewport:{width:1440,height:1000}});
const page = await context.newPage();
const errors = [];
page.on('pageerror', error => errors.push(error.message));
page.on('console', message => { if(message.type() === 'error') errors.push(message.text()); });
try {
 await page.goto('/login', {waitUntil:'domcontentloaded'});
 await page.locator('#loginName').fill(process.env.TEST_USER || 'admin');
 await page.locator('#password').fill(process.env.TEST_PASS || 'adminADMIN!');
 await page.locator('[data-cy="loginButton"]').click();
 await expect(page).toHaveURL(/\/$/, {timeout:60000});
 await page.goto('/order/environmental/enter', {waitUntil:'domcontentloaded'});
 await expect(page.locator('#sampleType-0')).toBeVisible();
 await expect(page.locator('#sample-site-0')).toBeVisible();
 await page.screenshot({path:resolve(evidence,'order-entry-after-upgrade.png'),fullPage:true});
 console.log('PASS: app login and order entry after the electronic/reflex build');
} finally {
 await writeFile(resolve(evidence,'console-errors.json'),JSON.stringify(errors,null,2));
 await context.close(); await browser.close();
}
