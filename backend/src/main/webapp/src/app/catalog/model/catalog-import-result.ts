export class ImportError {
  line!: number;
  message!: string;
}

export class CatalogImportResult {
  applied!: boolean;
  created!: number;
  updated!: number;
  errors!: ImportError[];
}
