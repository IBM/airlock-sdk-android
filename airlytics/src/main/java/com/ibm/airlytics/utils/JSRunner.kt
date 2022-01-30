package com.ibm.airlytics.utils

import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Scriptable

/**
 * Class runner for running Javascript commands and getting its results
 */
class JSRunner {
    companion object {
        private var contextFactory: ContextFactory? = null

        fun runJSBooleanCondition(conditionInput: String, condition: String?): Boolean {

            if (condition == null || condition.isEmpty() || condition == "true") {
                return true
            } else if (condition == "false") {
                return false
            }

            if (contextFactory == null) {
                contextFactory = ContextFactory.getGlobal()
            }
            var result: Any? = null

            contextFactory?.let {
                val context = it.enterContext()
                val scope: Scriptable
                context.optimizationLevel = -1
                scope = context.initStandardObjects()

                try {
                    context.evaluateString(scope, conditionInput, "<cmd>", 1, null)
                    result = context.evaluateString(scope, condition, "<cmd>", 1, null)
                } catch (th: Throwable) {
                    //If single stream processing fails do not stop processing of other streams...
                    //                            StreamTrace.getInstance().write("javascript processing failed for stream -  " + stream.getName() + ", and filter -" + stream.getFilter() + ": " + th.getMessage());
                }
            }
            return result as Boolean
        }

        //function to be used for running multiple conditions on one bulk
        @Suppress("unused") //good method to have and to use if needed in future
        fun runJSBooleanConditions(conditionInput: String, conditions: List<String>?): Boolean {

            if (conditions == null || conditions.isEmpty()) {
                return true
            }

            if (contextFactory == null) {
                contextFactory = ContextFactory.getGlobal()
            }
            var result: Any? = null

            contextFactory?.let {
                val context = it.enterContext()
                val scope: Scriptable
                context.optimizationLevel = -1
                scope = context.initStandardObjects()

                context.evaluateString(scope, conditionInput, "<cmd>", 1, null)
                for (condition in conditions) {
                    try {
                        result = context.evaluateString(scope, condition, "<cmd>", 1, null)
                    } catch (th: Throwable) {
                        //If single stream processing fails do not stop processing of other streams...
                        //                            StreamTrace.getInstance().write("javascript processing failed for stream -  " + stream.getName() + ", and filter -" + stream.getFilter() + ": " + th.getMessage());
                    }
                }
            }

            return result as Boolean
        }
    }
}