export type Units = "IMPERIAL" | "METRIC";

export const MM_PER_INCH = 25.4;
const SIXTEENTHS = 16;

/**
 * Formats a length in millimetres the way the Shop measures: inches to the
 * nearest 1/16" (e.g. 34 1/2") or whole millimetres.
 */
export function formatLength(mm: number | null | undefined, units: Units): string {
  if (mm === null || mm === undefined || Number.isNaN(mm)) {
    return "";
  }
  if (units === "METRIC") {
    return `${Math.round(mm)} mm`;
  }

  const sign = mm < 0 ? "-" : "";
  const sixteenths = Math.round((Math.abs(mm) / MM_PER_INCH) * SIXTEENTHS);
  const whole = Math.floor(sixteenths / SIXTEENTHS);
  let numerator = sixteenths % SIXTEENTHS;
  let denominator = SIXTEENTHS;
  while (numerator > 0 && numerator % 2 === 0) {
    numerator /= 2;
    denominator /= 2;
  }

  if (numerator === 0) {
    return `${sign}${whole}"`;
  }
  return whole === 0
    ? `${sign}${numerator}/${denominator}"`
    : `${sign}${whole} ${numerator}/${denominator}"`;
}

/**
 * Reads a length typed in the Shop's units. Imperial accepts 34, 34.5,
 * 34 1/2, 34-1/2 and 1/2, with or without a trailing ". Returns null when
 * the text is not a length.
 */
export function parseLength(text: string, units: Units): number | null {
  const value = text.trim().replace(/(mm|"|po|in)$/i, "").trim().replace(",", ".");
  if (value === "") {
    return null;
  }

  if (units === "METRIC") {
    const mm = Number(value);
    return Number.isFinite(mm) ? mm : null;
  }

  const match = /^(-?)(?:(\d+(?:\.\d+)?)(?:[\s-]+(\d+)\/(\d+))?|(\d+)\/(\d+))$/.exec(value);
  if (!match) {
    return null;
  }

  const [, sign, whole, numerator, denominator, onlyNumerator, onlyDenominator] = match;
  let inches = whole ? Number(whole) : 0;
  const top = numerator ?? onlyNumerator;
  const bottom = denominator ?? onlyDenominator;
  if (top && bottom) {
    if (Number(bottom) === 0) {
      return null;
    }
    inches += Number(top) / Number(bottom);
  }

  const mm = Math.round(inches * MM_PER_INCH * 10) / 10;
  return sign ? -mm : mm;
}
