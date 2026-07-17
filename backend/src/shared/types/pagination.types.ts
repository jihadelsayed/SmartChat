export interface PaginationQuery {
  page?: number;
  pageSize?: number;
}

export interface ResolvedPagination {
  page: number;
  pageSize: number;
  skip: number;
  take: number;
}

export interface PaginatedResult<T> {
  items: T[];
  pagination: {
    page: number;
    pageSize: number;
    totalItems: number;
    totalPages: number;
    hasPreviousPage: boolean;
    hasNextPage: boolean;
  };
}