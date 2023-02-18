# Alex's Full-Stack Quarkus Prototype

This prototype explores using Java + [Quarkus Reactive](https://quarkus.io/guides/getting-started-reactive) as a monolithic, fullstack application, where the frontend is rendered server-side with templates (via [Qute](https://quarkus.io/guides/qute-reference)), but with a modern interactive UI via [HTMX](https://htmx.org/) and [AlpineJS](https://alpinejs.dev/). This is a simple To-Do list app.

**HTMX**:
> htmx gives you access to AJAX, CSS Transitions, WebSockets and Server Sent Events directly in HTML, using attributes, so you can build modern user interfaces with the simplicity and power of hypertext

**Alpine**:
> Alpine is a rugged, minimal tool for composing behavior directly in your markup. Think of it like jQuery for the modern web.

At the outset of this project, I had only recently learned of these tools. I soon realized HTMX was one tool among many that fit a pattern of combining server-side rendering via templates, along with some client-side tool that handle AJAX calls to backend endpoints, fetching HTML rendered on the backend, and sticking it into the DOM. Other tools include [Turbo](https://turbo.hotwired.dev/), [Phoenix.LiveView](https://hexdocs.pm/phoenix_live_view/Phoenix.LiveView.html), and [Laravel Livewire](https://laravel-livewire.com/). On this [Hacker News thread](https://news.ycombinator.com/item?id=34530052), many sing the praises of this pattern.

This pattern is a promising way to increase developer productivity and velocity. Why? How? Well, there is an intuitive line of reasoning: if you can eliminate the need for a full frontend stack -- JavaScript, a framework, unit testing tools, debugging tools, NPM, webpack, dependency management, deployment, CI/CD, infrastructure concerns, telemetry -- and instead do all your work in a single monolithic backend, then you win a significant reduction in complexity. This reduces the context switch costs moving from backend to frontend. 

After building out the prototype, I'm convinced this pattern is a great fit for small scale apps: personal projects, prototypes, internal tools, and even MVPs. You could read the docs for both HTMX and AlpineJS in half a day, and with a few days of practice build enough skill to be quite productive.

## Development

When developing the app, I suggest starting out by running the shell script provided by Quarkus:

```shell script
./mvnw compile quarkus:dev
```

Just leave this running. It is incredibly useful for both quickly and automatically running tests on save (I don't even run tests from my IDE), as well as updating the UI after making changes.

There is also a DevUI (only in dev mode) which seems neat, although I haven't yet used it much:  http://localhost:8080/q/dev/.

## Useful Guides

- [Quarkus RESTEasy Reactive Reference](https://quarkus.io/guides/resteasy-reactive) (Writing endpoints)
- [Quarkus Hibernate Reactive Panache Reference](https://quarkus.io/guides/hibernate-reactive-panache) (Working with Hibernate)
- [Quarkus Qute Reference](https://quarkus.io/guides/qute-reference) (Templating)
- [HTMX Docs](https://htmx.org/docs/)
- [AlpineJS Docs](https://alpinejs.dev/start-here)

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.
