// FIR_IDENTICAL
// FIR_COMPARISON
package Tests

class A : java.<caret>

// EXIST: lang, util, io
// EXIST_K2: lang., util., io.
// ABSENT: fun, val, var, package
