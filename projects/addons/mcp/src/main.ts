import { App } from "./app.js";
import { mcpUrl, readEnv } from "./config.js";

const env = readEnv();
const app = new App({ env });
const server = await app.listen(env.port);
console.log(`openelis-addon-mcp listening on :${env.port}; clients connect to ${mcpUrl(env)}`);

const stop = () => {
  server.close();
  void app.close().finally(() => process.exit(0));
};
process.on("SIGTERM", stop);
process.on("SIGINT", stop);
