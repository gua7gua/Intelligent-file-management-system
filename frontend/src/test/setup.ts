import { config } from '@vue/test-utils'

config.global.stubs = {
  RouterLink: {
    props: ['to'],
    template: `<a :href="typeof to === 'string' ? to : to.path"><slot /></a>`,
  },
}
