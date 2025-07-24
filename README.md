# Hadoop Application Finder (HAF)

HAF is a Scala 3 CLI tool for searching and displaying Hadoop applications from ResourceManager, JobHistory, and SparkHistory servers. It supports filtering, colored output, and SPNEGO authentication for secure access.

## Features
- Query ResourceManager, JobHistory, and SparkHistory APIs
- Filter results by a search string
- Color and highlight matches in the console
- CLI argument parsing with [decline](https://github.com/com1monovore/decline)
- SPNEGO/Kerberos authentication support (requires kinit)
- Standalone fat jar build (sbt-assembly)

## Installation
1. **Clone the repository:**
   ```sh
   git clone <your-repo-url>
   cd haf
   ```
2. **Install SBT (Scala Build Tool):**
   - On macOS: `brew install sbt`
   - On Linux: Use your package manager or see [SBT installation docs](https://www.scala-sbt.org/download.html)
   - On Windows: Use [SBT installer](https://www.scala-sbt.org/download.html)
3. **Build the project:**
   ```sh
   sbt assembly
   ```
   This will create a standalone jar in `target/scala-3.7.1/haf-assembly-0.1.0-SNAPSHOT.jar`.

## Usage
1. Run `kinit` to authenticate with Kerberos if needed.
2. Run the CLI:
   ```sh
   java -jar target/scala-3.7.1/haf-assembly-0.1.0-SNAPSHOT.jar <filter> --rmHost <rm-host:port> --jhHost <jh-host:port> --shHost <sh-host:port>
   ```
   - `<filter>`: The search string to filter applications (positional argument)
   - `--rmHost`: ResourceManager host (e.g., `rm.example.com:8088`)
   - `--jhHost`: JobHistory host (e.g., `jh.example.com:19888`)
   - `--shHost`: SparkHistory host (e.g., `sh.example.com:18081`)

## Requirements
- Scala 3
- Kerberos authentication (if accessing secured endpoints)
- JVM with access to your Kerberos ticket cache

## Development
- Compile: `sbt compile`
- Test: `sbt test`
- Run: `sbt run`

## License
MIT
