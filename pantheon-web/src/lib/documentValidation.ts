/**
 * CPF/CNPJ format and check-digit validation (standard mod-11 algorithm), mirroring
 * `CpfValidator`/`CnpjValidator` on the backend so a bad document is caught before it's
 * submitted.
 */

function checkDigit(digits: number[], position: number, cnpj: boolean): number {
  let weight = cnpj ? position - 7 : position + 1
  let sum = 0
  for (let i = 0; i < position; i++) {
    sum += digits[i] * weight
    weight--
    if (cnpj && weight < 2) weight = 9
  }
  const remainder = sum % 11
  return remainder < 2 ? 0 : 11 - remainder
}

export function isValidCpf(raw: string): boolean {
  const digits = raw.replace(/\D/g, '')
  if (digits.length !== 11 || new Set(digits).size === 1) return false
  const d = digits.split('').map(Number)
  return d[9] === checkDigit(d, 9, false) && d[10] === checkDigit(d, 10, false)
}

export function isValidCnpj(raw: string): boolean {
  const digits = raw.replace(/\D/g, '')
  if (digits.length !== 14 || new Set(digits).size === 1) return false
  const d = digits.split('').map(Number)
  return d[12] === checkDigit(d, 12, true) && d[13] === checkDigit(d, 13, true)
}

/** Validates by digit count: 11 digits as a CPF, 14 as a CNPJ, anything else is invalid. */
export function isValidCpfOrCnpj(raw: string): boolean {
  const digits = raw.replace(/\D/g, '')
  if (digits.length === 11) return isValidCpf(raw)
  if (digits.length === 14) return isValidCnpj(raw)
  return false
}
