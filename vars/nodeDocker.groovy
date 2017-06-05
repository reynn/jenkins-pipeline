def call(Closure body) {
  node('docker') {
    ansiColor('xterm') {
      def ws = pwd()
      try {
        body()
      } catch (e) {
        throw e
      } finally {
        // Why you fail Jenkins
        dir(ws) {
          deleteDir()
        }
      }
    }
  }
}
