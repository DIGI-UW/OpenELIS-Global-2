import { transifexApi } from "@transifex/api";
import * as dotenv from "dotenv";

dotenv.config();
const token = process.env.API_TOKEN;
transifexApi.setup({ auth: token });

const fetchOrganizations = async () => {
  const organizations = transifexApi.Organization.list();
  await organizations.fetch();
  console.log(organizations);
};

fetchOrganizations();
