const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$/;

export const PASSWORD_HINT =
  "At least 8 characters, with an uppercase letter, a number, and a special character.";

export function isValidEmail(email) {
  return EMAIL_REGEX.test(email);
}

export function isValidPassword(password) {
  return PASSWORD_REGEX.test(password);
}
