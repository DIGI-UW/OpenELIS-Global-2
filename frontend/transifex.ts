import { transifexApi } from "@transifex/api";
import * as dotenv from "dotenv";

dotenv.config();
const token = process.env.API_TOKEN;
transifexApi.setup({ auth: token });

const fetchOrganization = async () => {
  const org = transifexApi.Organization.list();
  await org.fetch();
  console.log(org);
};

fetchOrganization();
