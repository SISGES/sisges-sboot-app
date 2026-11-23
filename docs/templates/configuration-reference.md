# [Component] configuration reference

Audience and scope — [developer or operator, service and environment]

Source baseline — [loader, deployment manifest and build revision]

## Configuration contract

| Setting | Read at build or runtime | Units, default and valid range | Required together with | Effect on the service |
| --- | --- | --- | --- | --- |
| [Name] | [When] | [Value or requirement] | [Dependency] | [Observable effect, including invalid or empty input] |

## Deployment differences

[Compare application defaults, image values and deployment overrides. Describe how to supply secret values using references or synthetic examples.]

## Verification

[Give a safe observable check for this configuration and the symptom to expect if it fails. Identify which changes require a rebuild, restart or coordinated rotation.]

## Review triggers

[Update when the loader, build arguments, deployment manifest or downstream contract changes.]
