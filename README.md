# Manage Scheduled Tasks (JetBrains IDEs)

An IntelliJ Platform plugin that helps you discover and browse methods annotated with `@Scheduled` in your Java/Spring projects. It provides a dedicated tool window to quickly inspect scheduled tasks, review their cron expressions, and navigate to source.

## Features

- Tool window named `Scheduled Tasks` anchored on the right side of the IDE.
- Scans the project for methods annotated with `@Scheduled` and lists them.
- One-click navigation to the method source.
- Displays method package, class, method name, and the cron expression (if present).
- Grouping toggle: switch between grouping by package or by class.
- Quick actions: search, expand all, collapse all.

## Compatibility

- Plugin id: `com.kivojenko.plugin.manage_scheduled_tasks`
- Since-build: 231+
- Target IDE for development: IntelliJ IDEA Ultimate 2025.2.3
- Requires bundled plugins: Java
- Project language level: Java 21

## Change Log

- v1.0.0: Initial release with core functionality (browse and navigate `@Scheduled` methods, grouping, search, expand/collapse).

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
