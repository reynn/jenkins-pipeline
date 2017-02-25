def call(Closure body) {
  node('docker') {
    ansiColor('xterm') {
      def ws = pwd()
      try {
        body()
      } catch (e) {
        throw e
      } finally {
        dir(ws) {
          deleteDir()
        }
      }
    }
  }
}
