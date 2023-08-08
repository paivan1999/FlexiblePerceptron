package functionPackage

typealias OT = OperandToken
typealias VT = VariableToken
typealias NT = NumberToken
val u = 1
fun cutExternBrackets(operand:OperandToken):String{
    return if (operand.tokenizer.tokens.size == 1 &&
        run {
            val firstToken = operand.tokenizer.GetToken()
            firstToken !is NumberToken ||firstToken.number >= 0
        }) {
        operand.asStr()
    } else {
        val operandAsStr = operand.asStr()
        operandAsStr.substring(1,operandAsStr.lastIndex)
    }
}
class UnpredictedError(s:String):Exception(s)
class IncorrectPlacementOfBrackets:Exception("incorrect placement of brackets (every left bracket should correspond to right bracket)")
class OperatorAtStartError(operatorName:String):Exception("there can't be operator $operatorName at the start of any operand")
class MissingOperandAfterOperator(operator:String):Exception(
    "there should be operand after each entry of operator $operator"
)
class AssociativeDirectionShouldBeEqual(globOperator:BinaryOperatorToken, localOperator:BinaryOperatorToken):Exception(
    "associative direction should be the same for all operators on the same level if builder is not poly but binary " +
    "(${globOperator.name} is ${if (globOperator.leftAssociative) "left" else "right"} associative)" +
    "and ${localOperator.name} is ${if (localOperator.leftAssociative) "left" else "right"} associative"
)
open class PolyArgsError(s:String):Exception("poly args operator function $s")
class PolyArgsForSameOperatorsError(operator:NameToken, globOperator:NameToken):PolyArgsError(
    "can be built only if all operators on the same level are the same, but ${operator.name} and ${globOperator.name} are different"
)

class SecondTokenShouldBeOperator(token:Token):Exception(
    "second token ${run {
        try {
            token.asStr()
        } catch (e:Exception) {
            "'can't resolve name'"
        }
    }} should be operator but it isn't"
)
class UnaryOperatorIsNotOperand(operatorName:String):Exception("unary operator $operatorName can't be operand")

class EmptyTokenError:Exception("empty token () is incorrect token")
class OnlyUnaryOperatorAppliesToOneArgument(firstToken:Token):Exception(
    "if operand token consists of two tokens than first token should be unary operator but not this: ${run {
        try {
            firstToken.asStr()
        } catch (e:Exception) {
            "'can't resolve name'"
        }
    }}, so that" +
            "1(functionPackage.getX) can't have sense (or for example functionPackage.getX(1))"
) //TODO sqrt how works if it is not present in unary_func_map
//val t:FixedElementary = Tan

val smth_senseless:HashMap<String, FixedElementary> = hashMapOf("" to Sum(2))
val unary_func_map:HashMap<String, FixedElementary> = hashMapOf(
    "sin" to Sin,
    "cos" to Cos,
    "tan" to Tan,
    "cotan" to Cotan,
    "sec" to Sec,
    "cosec" to Cosec,
    "asin" to ASin,
    "acos" to ACos,
    "atan" to ATan,
    "ln" to Ln,
    "exp" to Exp
)
val binary_func_map = hashMapOf(
    "+" to BinarySum,
    "*" to BinaryProd,
    "^" to BinaryPow
)
val poly_func_map:HashMap<String, Elementary> = hashMapOf(
    "+" to PolySum,
    "*" to PolyProd
)

val binary_operators = setOf("+","-","/","*","^")
//val binary_operators_1_left = setOf<String>("+","-")
//val binary_operators_2_left = setOf<String>("*","/")
//val binary_operators_3_right = setOf<String>("^")
val unary_operators = setOf("sin","cos","sec", "cosec","tan","cotan","asin","acos","atan","ln","sqrt","exp")

val binary_operators_map = hashMapOf(
    "+" to BinaryOperatorToken("+",1,true),
    "-" to BinaryOperatorToken("-",1,true),
    "*" to BinaryOperatorToken("*",2,true),
    "/" to BinaryOperatorToken("/",2,true),
    "^" to BinaryOperatorToken("^",3,false)
)
val unary_operators_map:HashMap<String,UnaryOperatorToken> = run {
    val result = hashMapOf<String,UnaryOperatorToken>()
    for (name in unary_operators){
        result[name] = UnaryOperatorToken(name)
    }
    result
}

fun getPriorityByTokens(tokens:ArrayDeque<Token>, initialPriority:Int):Int{
    val token = tokens[0]
    return if (tokens.size == 1 && token is OperandToken && token.getDividedByPriority() != 0) {
        token.getDividedByPriority()
    } else {
        initialPriority
    }
}
class OperandToken(initialTokenizer:Tokenizer, private var priority:Int = 0):Token(){
    constructor(tokens:List<Token>,priority: Int=0):this(Tokenizer(tokens),priority)
    constructor(vararg tokens:Token,priority: Int=0):this(Tokenizer(*tokens),priority)
    constructor(varName:String, priority: Int=0):this(VariableToken(varName),priority=priority)
    constructor(number:Double,priority: Int=0):this(NumberToken(number),priority=priority)
    val tokenizer:Tokenizer
    fun getDividedByPriority() = priority
    fun setDividedByPriority(priority:Int){
        this.priority = priority
    }
    init {
        tokenizer = if (initialTokenizer.tokens.size == 1){
            val firstToken = initialTokenizer.tokens[0]
            if (firstToken is OperandToken){
                firstToken.tokenizer
            } else {
                initialTokenizer
            }
        } else {
            initialTokenizer
        }
    }
    operator fun times(other:Double):OperandToken{
        return OperandToken(Tokenizer(OperandToken(tokenizer,2),
            binary_operators_map["*"]!!,
            OperandToken(Tokenizer(NumberToken(other)),2)
        ),this.priority)
    }

    override fun equals(other: Any?): Boolean {
        return other is OperandToken && priority == other.priority && tokenizer == other.tokenizer
    }
//    fun Pow(other:Double):OperandToken{
//        return OperandToken(Tokenizer(
//            OperandToken(tokenizer,3),
//            binary_operators_map["^"]!!,
//            OperandToken(Tokenizer(NumberToken(other)),3)
//        ),priority)
//    }
//    private fun get_inner_priority():Int{
//        val token = tokenizer.tokens[0]
//        return if (token is OperandToken) token.priority else 0
//    }
    override fun asStr():String{//put_in_brackets:Boolean
        if (tokenizer.tokens.size == 1){
            val oneToken = tokenizer.tokens[0]
            return if (oneToken is NumberToken && oneToken.number < 0){
                "("+oneToken.asStr()+")"
            } else {
                oneToken.asStr()
            }
        } else {
            var result = ""
//            var inner_priority = get_inner_priority()
            for (token in tokenizer.tokens) {
                result+=token.asStr()
//                if (token is OperandToken) {
//                    var inner_inner_priority = token.get_inner_priority()
//                    var next_put_in_brackets: Boolean
//                    if (inner_inner_priority == 0 && inner_priority > 0) {
//                        next_put_in_brackets = false
//                    } else if (inner_priority == 0) {
//                        next_put_in_brackets = true
//                    } else {
//                        next_put_in_brackets = inner_inner_priority <= inner_priority
//                    }
//                    result += token.asStr(next_put_in_brackets)
//                } else {
//                    result += token.asStr()
//                }
            }
//            return if (put_in_brackets) "(" + result + ")" else result
            return "($result)"
        }
    }

    override fun hashCode(): Int {
        return tokenizer.hashCode()
    }

//    override fun asStr(): String {
//        throw Exception("should not call this")
//    }
}
open class BinaryOperatorToken(name:String,val priority:Int,val leftAssociative: Boolean):NameToken(name)

open class UnaryOperatorToken(name:String):NameToken(name)
class VariableToken(name:String):NameToken(name)
class Parser(private val tokenizer:Tokenizer, private val polyArgs: Boolean){
    constructor(s:String,polyArgs: Boolean):this(Tokenizer(s),polyArgs)
    fun parse(): MixedFuncNode {
        return parseAll(tokenizer,polyArgs)
    }
    companion object{
        fun prepareAll(tokenizer:Tokenizer):Tokenizer{
            val tokens = ArrayDeque<Token>()
            var token:Token
            while (!tokenizer.isEnd()){
                token = tokenizer.GetToken(true)
                if (token is NameToken){
                    if (unary_operators.contains(token.name)){
                        tokens.addLast(unary_operators_map[token.name]!!)
                    }
                    else if (binary_operators.contains(token.name)){
                        tokens.addLast(binary_operators_map[token.name]!!)
                    } else {
                        tokens.addLast(VT(token.name))
                    }
                }
                else {
                    tokens.addLast(token)
                }
            }
            return Tokenizer(tokens)
        }
        fun prepareAll(s:String):Tokenizer{
            return prepareAll(Tokenizer(s))
        }
        fun parseAllByBrackets(tokenizer:Tokenizer):OT{
            val innerTokenizer = Tokenizer(prepareAll(tokenizer).tokens + listOf<Token>(RightBracket()))
            val result = parseTokenizerByBracket(innerTokenizer)
            if (!innerTokenizer.isEnd()){
                throw IncorrectPlacementOfBrackets()
            }
            return result
        }
        fun parseAllByBrackets(s:String):OT{
            return parseAllByBrackets(Tokenizer(s))
        }
        fun parseTokenizerByBracket(tokenizer:Tokenizer):OT{
            val tokens = ArrayDeque<Token>()
            var token:Token
            while (!tokenizer.isEnd() && tokenizer.GetToken() !is RightBracket){
                token = tokenizer.GetToken(true)
                if (tokenizer.isEnd()){
                    throw IncorrectPlacementOfBrackets()
                }
                when (token) {
                    is LeftBracket -> tokens.addLast(parseTokenizerByBracket(tokenizer))
                    is RightBracket -> throw UnpredictedError("should end before it gets right bracket")
                    else -> tokens.addLast(token)
                }
            }
            if (tokenizer.isEnd()){
                throw IncorrectPlacementOfBrackets()
            }
            if (tokenizer.GetToken(true) !is RightBracket){
                throw UnpredictedError("should end with right bracket")
            }
            return OT(tokens)
        }
        fun parseAllByPriority(i: Int, tokenizer: Tokenizer): OT {
            //            println("input: ${s}, output: ${cut_extern_brackets(result)}")
            return parseOperandByPriority(parseAllByBrackets(tokenizer), i)
        }
        fun parseAllByPriority(i:Int,s:String):OT{
            return parseAllByPriority(i, Tokenizer(s))
        }
        fun parseAllByAllPriorities(tokenizer: Tokenizer):OT{
            var operand = parseAllByBrackets(tokenizer)
            for (i in 1..3){
                operand = parseOperandByPriority(operand,i)
            }
//            println("input: ${s}, output: ${cut_extern_brackets(operand)}")
            return operand
        }
        fun parseAllByAllPriorities(s:String):OT{
            return parseAllByAllPriorities(Tokenizer(s))
        }
        fun parseOperandByPriority(
            operandArg:OT,
            priority:Int
        ):OT{
            val tokenizer = operandArg.tokenizer
            val result = ArrayDeque<Token>()
            var tokens = ArrayDeque<Token>()
            if (tokenizer.isEmpty()) {
                throw EmptyTokenError()
            }
            val firstToken = tokenizer.GetToken(true)
            if (firstToken is BinaryOperatorToken){
                if (firstToken.name == "-"){
                    if (tokenizer.isEnd() || tokenizer.GetToken() is BinaryOperatorToken){
                        throw MissingOperandAfterOperator("-")
                    }
                    tokens.addLast(OT(-1.0))
                    tokens.addLast(binary_operators_map["*"]!!)
                } else {
                    throw OperatorAtStartError(firstToken.name)
                }
            } else {
                tokens.addLast(
                    if (firstToken is OT) parseOperandByPriority(firstToken,priority) else firstToken
                )
            }
            var token:Token
            while (!tokenizer.isEnd() && run{
                    val innerToken = tokenizer.GetToken()
                    !(innerToken is BinaryOperatorToken && innerToken.priority <= priority)
                }){
                token = tokenizer.GetToken(true)
                tokens.addLast(if (token is OT) parseOperandByPriority(token,priority) else token)
            }
            result.addLast(OT(tokens,getPriorityByTokens(tokens,priority)))
            while(!tokenizer.isEnd()){
                tokens = ArrayDeque()
                token = tokenizer.GetToken(true)
                if (!(token is BinaryOperatorToken && token.priority <= priority)) {
                    throw UnpredictedError("should be binary operator with no more priority than passed priority")
                }
//                val last = result.last() as OT
//                last.set_divided_by_priority(token.priority) // here token.priority is the same over all while cycle
                result.addLast(token)
                var innerToken:Token
                while (!tokenizer.isEnd() && run{
                        val innerInnerToken = tokenizer.GetToken()
                        !(innerInnerToken is BinaryOperatorToken && innerInnerToken.priority <= priority)
                    }){
                    innerToken = tokenizer.GetToken(true)
                    tokens.addLast(if (innerToken is OT) parseOperandByPriority(innerToken,priority) else innerToken)
                }
                if (tokens.isEmpty()){
                    throw MissingOperandAfterOperator(token.name)
                }
                result.addLast(OT(tokens,getPriorityByTokens(tokens,priority)))
            }
            return OT(result,operandArg.getDividedByPriority())
        }
        fun cleanAll(tokenizer: Tokenizer):OT{
            var result = parseAllByAllPriorities(tokenizer)
            result = cleanFromDivAndMinus(result)
            return result
        }
        fun cleanAll(s:String):OT{
            return cleanAll(Tokenizer(s))
        }
        fun cleanFromDivAndMinus(operandArg:OT):OT{
            val tokenizer = operandArg.tokenizer
            val tokens = ArrayDeque<Token>()
            var operand:Token
            while (!tokenizer.isEnd()){
                val token = tokenizer.GetToken(true)
                if (token is OT){
                    tokens.addLast(cleanFromDivAndMinus(token))
                } else {
                    if (token is BinaryOperatorToken){
                        if (token.name == "-"){
                            operand = tokenizer.GetToken(true)
                            if (operand is OT) {
                                tokens.addLast(binary_operators_map["+"]!!)
                                tokens.addLast(
                                    OT(
                                        OT(cleanFromDivAndMinus(operand), priority = 2),
                                        binary_operators_map["*"]!!,
                                        OT(-1.0,2),
                                        priority = operand.getDividedByPriority()
                                    )
                                )
                                operand.setDividedByPriority(2)
                            } else {
                                throw UnpredictedError("it should be Operand Token already")
                            }
                        } else if (token.name == "/"){
                            operand = tokenizer.GetToken(true)
                            if (operand is OT) {
                                tokens.addLast(binary_operators_map["*"]!!)
                                tokens.addLast(
                                    OT(
                                        OT(cleanFromDivAndMinus(operand), priority = 3),
                                        binary_operators_map["^"]!!,
                                        OT(-1.0,3),
                                        priority = operand.getDividedByPriority()
                                    )
                                )
                                operand.setDividedByPriority(3)
                            } else {
                                throw UnpredictedError("it should be Operand Token already")
                            }
                        } else {
                            tokens.addLast(token)
                        }
                    } else {
                        tokens.addLast(token)
                    }
                }
            }
            return OT(tokens,operandArg.getDividedByPriority())
        }
        private fun buildFunc(tokenizer:Tokenizer, polyArgs:Boolean): MixedFuncNode {
            tokenizer.Reset(0)
            val globOperator = tokenizer[1] as? BinaryOperatorToken
            var operand = tokenizer.GetToken(true)
            if (globOperator == null){
                tokenToFunc(tokenizer[1],polyArgs)
                throw SecondTokenShouldBeOperator(tokenizer[1])
            }
            var operator:Token
            if (polyArgs && globOperator.name != "^"){
                val operands = ArrayDeque<Token>()
                operands.addLast(operand)
                while (!tokenizer.isEnd()){
                    operator = tokenizer.GetToken(true)
                    if (operator !is BinaryOperatorToken){
                        throw UnpredictedError("all tokens on odd places should be binary_operators")
                    } else if (operator.name != globOperator.name){
                        throw PolyArgsForSameOperatorsError(operator,globOperator)
                    }
                    operand = tokenizer.GetToken(true)
                    operands.addLast(operand)
                }
                if (globOperator.name == "*" || globOperator.name == "+") {
                    return poly_func_map[globOperator.name]!!(
                        Args(operands.map{ tokenToFunc(it,true) }.toMutableList())
                    )
                }
                throw UnpredictedError("can't operate with operators other than +,*,^")
            } else {
                if (globOperator.leftAssociative){
                    var func = tokenToFunc(operand,polyArgs)
                    while(!tokenizer.isEnd()){
                        operator = tokenizer.GetToken(true)
                        if (operator !is BinaryOperatorToken){
                            throw UnpredictedError("all tokens on odd places should be binary_operators")
                        } else {
                            if (operator.priority != globOperator.priority){
                                throw UnpredictedError("there should be the same operators' priorities on the same level")
                            }
                            if (!operator.leftAssociative){
                                throw AssociativeDirectionShouldBeEqual(globOperator, operator)
                            }
                        }
                        operand = tokenizer.GetToken(true)
                        func = binary_func_map[operator.name]!!(func, tokenToFunc(operand,polyArgs))
                    }
                    return func
                } else {
                    val tokenizerReversed = Tokenizer(tokenizer.tokens.reversed())
                    operand = tokenizerReversed.GetToken(true)
                    var func = tokenToFunc(operand,polyArgs)
                    while(!tokenizerReversed.isEnd()){
                        operator = tokenizerReversed.GetToken(true)
                        if (operator !is BinaryOperatorToken){
                            throw UnpredictedError("all tokens on odd places should be binary_operators")
                        } else {
                            if (operator.priority != globOperator.priority){
                                throw UnpredictedError("there should be the same operators' priorities on the same level")
                            }
                            if (operator.leftAssociative){
                                throw AssociativeDirectionShouldBeEqual(globOperator, operator)
                            }
                        }
                        operand = tokenizerReversed.GetToken(true)
                        func = binary_func_map[operator.name]!!(tokenToFunc(operand,polyArgs),func)
                    }
                    return func
                }
            }
        }
        private fun tokenToFunc(token:Token, polyArgs: Boolean): MixedFuncNode {
            if (token is OT){
                return parseOperand(token,polyArgs)
            }
            if (token is NT){
                return Const(token.number)
            }
            if (token is VT){
                return Variable(Var(token.name))
            }
            if (token is UnaryOperatorToken){
                throw UnaryOperatorIsNotOperand(token.name)
            }
            if (token is BinaryOperatorToken){
                throw UnpredictedError(
                    "binary operator as operand should be impossible"
                )
            }
            throw UnpredictedError("can't parse other type of operand(number,variable or operand token)")
        }
        fun parseAll(tokenizer: Tokenizer, polyArgs: Boolean): MixedFuncNode {
            val result = parseOperand(cleanAll(tokenizer),polyArgs)
            println(result.asStr())
            return result
        }
        fun parseAll(s:String, polyArgs: Boolean): MixedFuncNode {
            return parseAll(Tokenizer(s),polyArgs)
        }
        fun parseOperand(operand:OT, polyArgs: Boolean = false): MixedFuncNode {
            val tokenizer = operand.tokenizer
            val firstToken = tokenizer.GetToken(true)
            if (tokenizer.Size() == 1){
                if (firstToken is OT){
                    throw UnpredictedError(
                        "can't be OT here because form OT(Tokenizer(OT(...))) should be impossible"
                    )
                }
                return tokenToFunc(firstToken,polyArgs)
            } else if (tokenizer.Size() == 2){
                val innerOperand = tokenizer.GetToken(true)
                if (firstToken is UnaryOperatorToken){
                    if (innerOperand !is OT){
                        throw UnpredictedError(
                            "inner operand should be operand token (not variable token or number token) " +
                                    "because it is in brackets, example: Sin(x)"
                        )
                    }
                    return unary_func_map[firstToken.name]!!(tokenToFunc(innerOperand,polyArgs))
                } else {
                    throw OnlyUnaryOperatorAppliesToOneArgument(firstToken)
                }
            } else {
                return buildFunc(tokenizer,polyArgs)
            }
        }
    }

}