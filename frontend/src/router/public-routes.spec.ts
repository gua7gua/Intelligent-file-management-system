import { describe, expect, it } from 'vitest'
import { publicRoutes } from './routes/public'

describe('publicRoutes', () => {
  it('includes forgot password route', () => {
    expect(publicRoutes.children?.some((route) => route.path === 'forgot-password')).toBe(true)
  })
})
