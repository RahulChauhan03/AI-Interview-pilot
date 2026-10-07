import { fileNameFromContentDisposition } from './download';

describe('fileNameFromContentDisposition', () => {
  it('reads a quoted file name', () => {
    expect(fileNameFromContentDisposition('attachment; filename="Asha_Rao_Acme_Resume.pdf"')).toBe('Asha_Rao_Acme_Resume.pdf');
  });

  it('prefers the encoded form', () => {
    expect(fileNameFromContentDisposition("attachment; filename=\"x.pdf\"; filename*=UTF-8''Zo%C3%AB_Resume.pdf")).toBe('Zoë_Resume.pdf');
  });

  it('returns null without a header', () => {
    expect(fileNameFromContentDisposition(null)).toBeNull();
  });
});
