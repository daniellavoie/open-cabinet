export class PageMetadata {
  size!: number;
  number!: number;
  totalElements!: number;
  totalPages!: number;
}

/**
 * A page of results, as Spring Data serializes it (PageSerializationMode.VIA_DTO).
 */
export class Page<T> {
  content!: T[];
  page!: PageMetadata;
}
