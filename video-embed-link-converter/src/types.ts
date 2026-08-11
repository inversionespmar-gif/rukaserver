/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export interface TraceStep {
  step: string;
  status: 'pending' | 'success' | 'failed' | 'info';
  message: string;
  timestamp: string;
}

export interface ConversionResult {
  success: boolean;
  url?: string;
  type?: 'm3u8' | 'mp4' | 'unknown';
  title?: string;
  methodUsed?: 'regex' | 'unpacker' | 'gemini_ai' | 'fallback';
  trace?: TraceStep[];
  allFoundUrls?: string[];
  headers?: Record<string, string>;
  error?: string;
}

export interface ConversionRequest {
  url: string;
  useAi: boolean;
}
