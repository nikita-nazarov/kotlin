// FIR_IDENTICAL
// RENDER_DIAGNOSTICS_FULL_TEXT

annotation class Ann1

@Target(AnnotationTarget.FUNCTION)
annotation class Ann2

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@property:Ann2<!>
val foo: <!WRONG_ANNOTATION_TARGET!>@Ann1<!> <!WRONG_ANNOTATION_TARGET!>@Ann2<!> String = ""
