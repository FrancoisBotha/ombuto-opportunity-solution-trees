import { describe, expect, it } from 'vitest';

import { describeError, httpStatus, loadFailure } from './ost-errors';

const apiError = (status: number | undefined, message?: string) => ({ response: { status, data: { message } } });

describe('OST error messages', () => {
  it('maps the API message keys to sentences', () => {
    expect(describeError(apiError(400, 'error.cycle'))).toBe('A node cannot move under its own descendant.');
    expect(describeError(apiError(400, 'error.unknowntype'))).toBe('That node type is not recognised.');
  });

  it('states the title rule of the node type when one is given', () => {
    const invalid = apiError(400, 'error.invalidtitle');
    expect(describeError(invalid, undefined, 'product')).toBe('Product names need 2 to 100 characters.');
    expect(describeError(invalid, undefined, 'opportunity')).toBe('Titles need 2 to 200 characters.');
    expect(describeError(invalid, undefined, 'evidence')).toBe('Titles need 2 to 500 characters.');
    expect(describeError(invalid)).toContain('100 for products');
  });

  it('explains the 409 conflicts', () => {
    expect(describeError(apiError(409, 'error.concurrencyFailure'))).toBe(
      'Someone else changed this tree at the same moment — please try again.',
    );
    expect(describeError(apiError(409, 'error.dataintegrity'))).toBe(
      'Something else still refers to this, so the change could not be made.',
    );
  });

  it('no longer knows the retired nodetypeinvalid key', () => {
    expect(describeError(apiError(400, 'error.nodetypeinvalid'), 'fallback')).toBe('fallback');
  });

  it('falls back by status, then to the given sentence', () => {
    expect(describeError(apiError(403))).toBe('You do not have permission to change this tree.');
    expect(describeError(apiError(404, 'error.somethingelse'))).toBe('That item no longer exists.');
    expect(describeError(apiError(500), 'Could not save.')).toBe('Could not save.');
    expect(describeError(new Error('network'))).toBe('Something went wrong. Your change was not saved.');
    expect(describeError(undefined, 'Offline.')).toBe('Offline.');
  });

  it('classifies load failures', () => {
    expect(httpStatus({ status: 418 })).toBe(418);
    expect(loadFailure(apiError(401))).toBe('forbidden');
    expect(loadFailure(apiError(403))).toBe('forbidden');
    expect(loadFailure(apiError(400))).toBe('notFound');
    expect(loadFailure(apiError(404))).toBe('notFound');
    expect(loadFailure(apiError(500))).toBe('error');
    expect(loadFailure(new Error('x'))).toBe('error');
  });
});
