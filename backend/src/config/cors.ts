import type { CorsOptions } from "cors";
import { environment } from "./environment";

export const corsOptions: CorsOptions = {
  origin: environment.CORS_ORIGIN === "*"
    ? true
    : environment.CORS_ORIGIN.split(",").map((origin) => origin.trim()),
  credentials: true
};
