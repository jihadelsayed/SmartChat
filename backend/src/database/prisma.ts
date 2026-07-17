import { PrismaPg } from "@prisma/adapter-pg";
import { PrismaClient } from "../generated/prisma/client";
import { environment } from "../config/environment";

const adapter = new PrismaPg({ connectionString: environment.DATABASE_URL });

export const prisma = new PrismaClient({ adapter });
