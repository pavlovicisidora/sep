export class LuhnValidator {
  
  static validatePan(pan: string): boolean {
    if (!pan || !/^\d{13,19}$/.test(pan)) {
      return false;
    }

    let sum = 0;
    let alternate = false;

    for (let i = pan.length - 1; i >= 0; i--) {
      let digit = parseInt(pan.charAt(i), 10);

      if (alternate) {
        digit *= 2;
        if (digit > 9) {
          digit -= 9;
        }
      }

      sum += digit;
      alternate = !alternate;
    }

    return sum % 10 === 0;
  }

  static detectCardType(pan: string): 'visa' | 'mastercard' | 'amex' | 'dinners' | 'unknown' {
    if (!pan) return 'unknown';

    // Visa
    if (pan.startsWith('4')) {
      return 'visa';
    }

    // Mastercard: 51-55 or 2221-2720
    if (/^5[1-5]/.test(pan) || 
        (pan.length >= 4 && parseInt(pan.substring(0, 4)) >= 2221 
         && parseInt(pan.substring(0, 4)) <= 2720)) {
      return 'mastercard';
    }

    // American Express: 34 or 37
    if (/^3[47]/.test(pan)) {
      return 'amex';
    }

    // Diners Club: 36 or 38
    if (/^3[68]/.test(pan)) {
      return 'dinners';
    }

    return 'unknown';
  }

  static formatPan(pan: string): string {
    return pan.replace(/\s/g, '').replace(/(\d{4})/g, '$1 ').trim();
  }
}
