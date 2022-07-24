/**
 * @type {import('vite').UserConfig}
 */

import path from 'path'

export default {
  root: path.resolve(__dirname, './../modules/frontend/target/js-3/'),
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true,
      }
    }
  }
}
