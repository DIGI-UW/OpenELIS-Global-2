import { transifexApi } from "@transifex/api";
import * as dotenv from "dotenv";

dotenv.config();
const token = process.env.API_TOKEN;
transifexApi.setup({ auth: token });

const fetchOrganizations = async () => {
  const orgs = transifexApi.Organization.list();
  await orgs.fetch();
  console.log(orgs);
};

fetchOrganizations();
