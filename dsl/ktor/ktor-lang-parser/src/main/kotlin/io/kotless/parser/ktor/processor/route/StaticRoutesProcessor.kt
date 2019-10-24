package io.kotless.parser.ktor.processor.route

import io.kotless.*
import io.kotless.dsl.ktor.Kotless
import io.kotless.parser.ktor.utils.toMime
import io.kotless.parser.processor.ProcessorContext
import io.kotless.parser.processor.SubTypesProcessor
import io.kotless.parser.utils.psi.utils.gatherCallsOf
import io.kotless.parser.utils.psi.utils.gatherNamedFunctions
import io.kotless.utils.TypedStorage
import io.ktor.http.ContentType
import io.ktor.http.defaultForFile
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import java.io.File

internal object StaticRoutesProcessor : SubTypesProcessor<Unit>() {
    override val klasses = setOf(Kotless::class)

    override fun mayRun(context: ProcessorContext) = true

    override fun process(files: Set<KtFile>, binding: BindingContext, context: ProcessorContext) {

        processClasses(files, binding) { klass, _ ->
            klass.gatherNamedFunctions { func -> func.name == Kotless::prepare.name }.forEach {
                for (expr in it.gatherCallsOf("io.ktor.http.content.static", binding)) {
                    val outer = getPath(expr, binding)
                    for (fileCall in expr.gatherCallsOf("io.ktor.http.content.file", binding)) {
                        val (file, innerPath) = getFileValue(fileCall, binding, context.config.workDirectory)

                        val path = URIPath(outer, innerPath)

                        val key = TypedStorage.Key<StaticResource>()
                        val resource = StaticResource(context.config.bucket, URIPath("static", path), file, MimeType.forFile(file)
                            ?: ContentType.defaultForFile(file).toMime()!!)

                        context.resources.register(key, resource)
                        context.routes.register(Webapp.ApiGateway.StaticRoute(path, key))
                    }
                }
            }
        }
    }

    private fun getPath(expr: KtCallExpression, binding: BindingContext): URIPath {
        if (expr.valueArguments.size == 1) return URIPath()
        val arg = expr.valueArguments.firstOrNull()?.getArgumentExpression() ?: return URIPath()

        val value = ConstantExpressionEvaluator.getConstant(arg, binding)
        require(value is TypedCompileTimeConstant && value.type.nameIfStandardType?.identifier == "String") {
            "Static routing path should be compile-time constant string"
        }
        val path = value.constantValue.value as String

        return URIPath(path.split("/"))
    }

    private fun getFileValue(expr: KtCallExpression, binding: BindingContext, dir: File): Pair<File, URIPath> {
        require(expr.valueArguments.size == 2) {
            "All static routing functions should have path and local argument"
        }
        val (path, file) = expr.valueArguments.map { it.getArgumentExpression() }
        require(file != null && path != null) {
            "Static routing path should be compile-time constant string"
        }
        val fileValue = ConstantExpressionEvaluator.getConstant(file, binding)
        val pathValue = ConstantExpressionEvaluator.getConstant(path, binding)
        require(fileValue is TypedCompileTimeConstant && fileValue.type.nameIfStandardType?.identifier == "String" &&
            pathValue is TypedCompileTimeConstant && pathValue.type.nameIfStandardType?.identifier == "String") {
            "Static routing path should be compile-time constant string"
        }

        return File(dir, fileValue.constantValue.value as String) to URIPath((pathValue.constantValue.value as String).split("/"))
    }

}