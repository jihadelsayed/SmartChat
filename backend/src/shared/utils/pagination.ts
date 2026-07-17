import type {
  PaginationQuery,
  ResolvedPagination
} from "../types/pagination.types";

const DEFAULT_PAGE = 1;
const DEFAULT_PAGE_SIZE = 20;
const MAXIMUM_PAGE_SIZE = 100;

export function resolvePagination(
  query: PaginationQuery
): ResolvedPagination {
  const requestedPage = Number(query.page ?? DEFAULT_PAGE);
  const requestedPageSize = Number(
    query.pageSize ?? DEFAULT_PAGE_SIZE
  );

  const page =
    Number.isInteger(requestedPage) && requestedPage > 0
      ? requestedPage
      : DEFAULT_PAGE;

  const pageSize =
    Number.isInteger(requestedPageSize) && requestedPageSize > 0
      ? Math.min(requestedPageSize, MAXIMUM_PAGE_SIZE)
      : DEFAULT_PAGE_SIZE;

  return {
    page,
    pageSize,
    skip: (page - 1) * pageSize,
    take: pageSize
  };
}