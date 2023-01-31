INSERT INTO public.user (id, name)
VALUES ('ed8e4579-ba54-4441-8dee-07eb25d57e4b', 'Alex Graham');

INSERT INTO public.user (id, name)
VALUES ('298eef26-8897-4aee-8ada-3cb82e7b0900', 'Colorado Slim');

INSERT INTO public.task(id, title, ownerid)
VALUES (nextval('hibernate_sequence'), 'build create task endpoint', 'ed8e4579-ba54-4441-8dee-07eb25d57e4b');

INSERT INTO public.task(id, title, ownerid)
VALUES (nextval('hibernate_sequence'), 'build list tasks endpoint','ed8e4579-ba54-4441-8dee-07eb25d57e4b');

INSERT INTO public.task(id, title, ownerid)
VALUES (nextval('hibernate_sequence'), 'build get task endpoint', 'ed8e4579-ba54-4441-8dee-07eb25d57e4b');

INSERT INTO public.task(id, title, ownerid)
VALUES (nextval('hibernate_sequence'), 'build edit task endpoint', 'ed8e4579-ba54-4441-8dee-07eb25d57e4b');

INSERT INTO public.task(id, title, ownerid)
VALUES (nextval('hibernate_sequence'), 'be awesome', '298eef26-8897-4aee-8ada-3cb82e7b0900');