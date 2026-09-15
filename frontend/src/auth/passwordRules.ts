/**
 * The same rules the API enforces, restated for the sign-up form.
 *
 * This is a duplicate of server logic, and deliberately so: it exists to show the requirements
 * while someone types, not to decide anything. The server remains the only thing that grants an
 * account, and its message is what the form shows if the two ever disagree.
 */

export interface PasswordRule {
  label: string;
  satisfied: (password: string) => boolean;
}

export const PASSWORD_RULES: PasswordRule[] = [
  { label: 'At least 10 characters', satisfied: (p) => p.length >= 10 },
  { label: 'A lowercase letter', satisfied: (p) => /\p{Ll}/u.test(p) },
  { label: 'An uppercase letter', satisfied: (p) => /\p{Lu}/u.test(p) },
  { label: 'A digit', satisfied: (p) => /\d/.test(p) },
  {
    label: 'A symbol, such as _ ! ? or -',
    satisfied: (p) => /[^\p{L}\p{N}\s]/u.test(p),
  },
  { label: 'No spaces', satisfied: (p) => p.length > 0 && !/\s/.test(p) },
];

export function unmetRules(password: string): PasswordRule[] {
  return PASSWORD_RULES.filter((rule) => !rule.satisfied(password));
}
