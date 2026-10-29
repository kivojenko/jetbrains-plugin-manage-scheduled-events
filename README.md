# Manage Scheduled Events (JetBrains IDEs)

An IntelliJ Platform plugin that shows every scheduled job in your Java/Spring project in one place: Spring
`@Scheduled` methods and Quartz jobs. It provides a dedicated tool window to inspect jobs, read and edit their crons,
and navigate to source.

## Features

- Tool window named `Scheduled Events` anchored on the right side of the IDE.
- Lists Spring `@Scheduled` methods and Quartz jobs (`Job` / `QuartzJobBean`) with their cron triggers.
- Describes each cron in words and flags invalid ones, using Spring or Quartz cron rules as appropriate.
- Edit a cron in place; it is written back to the code or config file it comes from.
- Follows crons into `application.yml` / `.properties` through `${...}` placeholders, `@Value` and
  `@ConfigurationProperties`, and shows profile overrides.
- Double-click to navigate to source.
- Grouping: by class, by package, or a flat list of jobs.
- Sorting: by name, or by how often jobs run.
- Quick actions: refresh, expand all, collapse all.
- Optional ShedLock helpers: add/remove `@SchedulerLock` to all `@Scheduled` methods when ShedLock is present on the
  classpath.

## Quartz Support

When Quartz is on the classpath, the tool window also lists every non-abstract class implementing `org.quartz.Job`
(including Spring's `QuartzJobBean`), grouped by job class, one entry per cron trigger.

Cron triggers are found wherever a cron string is passed to:

- `CronScheduleBuilder.cronSchedule(...)` / `cronScheduleNonvalidatedExpression(...)`
- `CronTriggerFactoryBean.setCronExpression(...)`
- `CronTriggerImpl.setCronExpression(...)`

A trigger is matched to its job by a `MyJob.class` literal in the same method, in a method it calls (e.g.
`forJob(myJobDetail())`), or in the `@Bean` method a parameter is named after (e.g. `myTrigger(JobDetail myJobDetail)`).
Triggers whose job can't be resolved are listed under the class that defines them; jobs with no cron trigger are listed
as `(no cron trigger)`.

Quartz crons are parsed with Quartz rules (e.g. optional year field, `L`, `W`, `#`). ShedLock actions don't apply to
Quartz jobs.

## Crons from application.yml / .properties

Crons don't have to be written in code. The plugin follows them into `application.yml`, `application.yaml` and
`application.properties` (test resources are ignored) when they are read through:

- a placeholder in `@Scheduled`: `@Scheduled(cron = "${jobs.backup.cron}")`, including `${key:default}`
- `@Value` on a field or parameter: `@Value("${jobs.archive-cron}") String cron`
- getters on a `@ConfigurationProperties` class: `cronSchedule(jobs.getReport().getCron())` on
  `@ConfigurationProperties("jobs")` reads `jobs.report.cron`

Keys are matched with Spring's relaxed binding, as nested YAML, dotted keys or a mix of both. The tree shows the
default-profile value, with `.properties` taking precedence over YAML as in Spring Boot. Profile files
(`application-prod.yml`) and profile documents (`spring.config.activate.on-profile`) that set a different value are
listed next to it, e.g. `jobs.report.cron (prod overrides)`. If the key isn't set anywhere, the placeholder's default is
used; without one the entry is shown as unresolved.

Editing a cron writes it where it comes from:

- a string literal in code is rewritten
- a property is changed in the config file that sets its default-profile value
- a property that isn't set anywhere has its placeholder default rewritten, e.g. `${jobs.digest.cron:0 0 8 * * *}`

Crons from shared constants, and properties with no value or default, are shown but not editable.

## Compatibility

- Plugin id: `com.kivojenko.plugin.manage-scheduled-events`
- Since-build: 242+
- Target IDE for development: IntelliJ IDEA Ultimate 2025.2.3
- Requires bundled plugins: Java, Properties, YAML
- Project language level: Java 21

## Change Log

- v1.1.0:
  - Quartz support: lists `Job` / `QuartzJobBean` classes with their cron triggers, validated and described with Quartz
    cron rules (year field, `L`, `W`, `#`).
  - Crons from `application.yml` / `.properties`: `${...}` placeholders in `@Scheduled`, `@Value` and
    `@ConfigurationProperties` getters are resolved, including `${key:default}` and profile overrides.
  - Editing a cron that comes from a config file updates the config file.
  - Crons from constants are now shown, e.g. `@Scheduled(cron = DAILY)`.
  - `@Scheduled(fixedRate/fixedDelay)` methods show `<no cron>` instead of an empty cron.
  - Now requires the bundled YAML and Properties plugins.
- v1.0.1:
  - Sorting: switch between sorting by name and by frequency (most often running jobs first).
  - New "Jobs only" grouping that shows a flat list of scheduled methods.
  - ShedLock actions are hidden when ShedLock is not on the classpath.
  - Fixed a crash when a scheduled method was edited or removed while listed; a warning is now shown if a cron change
    can't be saved.
- v1.0.0: Initial release with core functionality (browse and navigate `@Scheduled` methods, grouping, search, expand/collapse). ShedLock support (toolbar actions to add/remove `@SchedulerLock` annotations; quick navigation to enable ShedLock in your project).

## ShedLock Support

ShedLock is a library that guarantees that scheduled tasks run only on one instance in a clustered environment. If your application runs on multiple nodes and uses Spring `@Scheduled` tasks, consider enabling ShedLock to prevent concurrent executions.

What the plugin provides:

- Detects presence of ShedLock on the classpath.
- Toolbar buttons in the Scheduled Events tool window to:
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
