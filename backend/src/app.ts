import path from "node:path";
import cors from "cors";
import express from "express";
import helmet from "helmet";
import morgan from "morgan";
import { API_PREFIX } from "./config/constants";
import { corsOptions } from "./config/cors";
import { errorHandler } from "./middleware/error.middleware";
import { notFoundHandler } from "./middleware/not-found.middleware";
import { requestLogger } from "./middleware/request-logger.middleware";
import { apiRouter } from "./routes";
import { successResponse } from "./shared/responses/api-response";

export const app = express();

const uploadsDirectory = path.resolve(process.cwd(), "uploads");

app.disable("x-powered-by");

app.use(
  helmet({
    crossOriginResourcePolicy: {
      policy: "cross-origin"
    }
  })
);

app.use(cors(corsOptions));
app.use(express.json({ limit: "1mb" }));
app.use(express.urlencoded({ extended: true }));
app.use(requestLogger);
app.use(morgan("dev"));

app.use(
  "/uploads",
  express.static(uploadsDirectory, {
    fallthrough: false,
    index: false,
    maxAge: "1h"
  })
);

app.get("/api/health", (_request, response) => {
  response.json(
    successResponse({
      status: "ok",
      service: "smartchat-backend"
    })
  );
});

app.use(API_PREFIX, apiRouter);

app.use(notFoundHandler);
app.use(errorHandler);
