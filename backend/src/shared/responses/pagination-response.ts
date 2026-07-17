import type { PaginatedResult } from "../types/pagination.types";

export function createPaginatedResponse<T>(
  items: T[],
  page: number,
  pageSize: number,
  totalItems: number
): PaginatedResult<T> {
  const totalPages =
    totalItems === 0 ? 0 : Math.ceil(totalItems / pageSize);

  return {
    items,
    pagination: {
      page,
      pageSize,
      totalItems,
      totalPages,
      hasPreviousPage: page > 1,
      hasNextPage: page < totalPages
    }
  };
}