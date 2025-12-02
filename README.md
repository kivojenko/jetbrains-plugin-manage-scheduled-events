# Manage Scheduled Tasks (JetBrains IDEs)

An IntelliJ Platform plugin that helps you discover and browse methods annotated with `@Scheduled` in your Java/Spring projects. It provides a dedicated tool window to quickly inspect scheduled tasks, review their cron expressions, and navigate to source.

## Features

- Tool window named `Scheduled Tasks` anchored on the right side of the IDE.
- Scans the project for methods annotated with `@Scheduled` and lists them.
- One-click navigation to the method source.
- Displays method package, class, method name, and the cron expression (if present).
- Grouping toggle: switch between grouping by package or by class.
- Quick actions: search, expand all, collapse all.
- Optional ShedLock helpers: add/remove `@SchedulerLock` to all listed methods when ShedLock is present on the classpath.

## Compatibility

- Plugin id: `com.kivojenko.plugin.manage_scheduled_events`
- Since-build: 242+
- Target IDE for development: IntelliJ IDEA Ultimate 2025.2.3
- Requires bundled plugins: Java
- Project language level: Java 21

## Change Log

- v1.0.0: Initial release with core functionality (browse and navigate `@Scheduled` methods, grouping, search, expand/collapse). ShedLock support (toolbar actions to add/remove `@SchedulerLock` annotations; quick navigation to enable ShedLock in your project).

## ShedLock Support

ShedLock is a library that guarantees that scheduled tasks run only on one instance in a clustered environment. If your application runs on multiple nodes and uses Spring `@Scheduled` tasks, consider enabling ShedLock to prevent concurrent executions.

What the plugin provides:

- Detects presence of ShedLock on the classpath.
- Toolbar buttons in the Scheduled Tasks tool window to:
  - Lock All: add `@SchedulerLock` to every listed `@Scheduled` method. The plugin adds an import for `net.javacrumbs.shedlock.spring.annotation.SchedulerLock` and inserts an annotation like:

  ```text
  @SchedulerLock(name = "ClassName_methodNameLock")
  ```

  directly after the `@Scheduled` annotation on a method. You can edit or fine‑tune parameters (e.g., `lockAtLeastFor`, `lockAtMostFor`) manually as needed.

    - Unlock All: remove `@SchedulerLock` from listed methods and clean up redundant imports.

    
Prerequisites in your project:

- Add ShedLock to your build and configure a storage provider.
- Configure the chosen provider (DataSource, Mongo, Redis, etc.) per ShedLock docs.
- Make sure Spring scheduling is enabled (e.g., `@EnableScheduling`).

Useful links:

- ShedLock project: https://github.com/lukas-krecan/ShedLock
- Annotation reference: `net.javacrumbs.shedlock.spring.annotation.SchedulerLock`

## Feedback and Contributions

Your feedback is essential for improving this plugin!

### Ways to Get Involved:

- Report Bugs: Please create an issue in this repository’s issues section.
- Request Features: Have an idea to make this plugin even better? Open a feature request.
- Contribute Code: Pull requests are welcome.

### Contact

For any questions or direct feedback, feel free to reach me at:
📧 kivojenko@gmail.com

## License

This plugin is open-source and licensed under the [Apache License 2.0](LICENSE).
