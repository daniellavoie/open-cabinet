/**
 * Hands a downloaded file to the browser to save under `filename`.
 */
export function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

/**
 * The language printed outputs are written in: the one the app is read in.
 */
export function outputLanguage(locale: string): "fr" | "en" {
  return locale.startsWith("en") ? "en" : "fr";
}
