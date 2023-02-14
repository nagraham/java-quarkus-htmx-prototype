INSERT INTO public.user (id, name)
VALUES ('ed8e4579-ba54-4441-8dee-07eb25d57e4b', 'Alex Graham');

INSERT INTO public.user (id, name)
VALUES ('298eef26-8897-4aee-8ada-3cb82e7b0900', 'Colorado Slim');

INSERT INTO public.task(id, title, ownerid, state, description)
VALUES (nextval('hibernate_sequence'), '[A] Build create task endpoint', 'ed8e4579-ba54-4441-8dee-07eb25d57e4b', 'Open',
        'The sky above the port was the color of a television tuned to a dead channel.');

INSERT INTO public.task(id, title, ownerid, state)
VALUES (nextval('hibernate_sequence'), '[B] build list tasks endpoint','ed8e4579-ba54-4441-8dee-07eb25d57e4b', 'Open');

INSERT INTO public.task(id, title, ownerid, state, description)
VALUES (nextval('hibernate_sequence'), '[C] build get task endpoint', 'ed8e4579-ba54-4441-8dee-07eb25d57e4b', 'Open',
        'Some description of something that was once but does no longer remains of this world like a sea of sand of what was once ruins of what was once some place some people had once lived.');

INSERT INTO public.task(id, title, ownerid, state)
VALUES (nextval('hibernate_sequence'), '[D] build edit task endpoint', 'ed8e4579-ba54-4441-8dee-07eb25d57e4b', 'Open');

INSERT INTO public.task(id, title, ownerid, state)
VALUES (nextval('hibernate_sequence'), 'be awesome', '298eef26-8897-4aee-8ada-3cb82e7b0900', 'Open');