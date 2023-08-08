package functionPackage

import java.io.StringReader
import kotlin.math.round

class IncorrectTokenException(s:String):Exception(s)

val reserved_symbs = "()+\\-/*^"
val reserved_symbs_set:Set<String> = setOf(*reserved_symbs.toCharArray().map{it -> it.toString()}.toTypedArray())
fun StringReader.peek():Char{
    this.mark(1)
    var result:Char = this.read1()
    this.reset()
    return result
}
fun StringReader.read1():Char{
    return this.read().toChar()
}
fun Split(delimiters:String,s:String):List<String>{
    if (s.isEmpty()){
        return listOf()
    }
    var result = s.replace("\\s+".toRegex(), "")
        .split((Regex("(?<=[${delimiters}])|(?=[${delimiters}])|(?>=[${delimiters}])")))
    return result.subList(if (result[0].length == 0) 1 else 0,
        if (result.last().length == 0) result.lastIndex else result.size)
}

abstract class Token{
    abstract fun asStr():String
    abstract override fun equals(other: Any?):Boolean
}
class LeftBracket:Token(){
    override fun equals(other: Any?): Boolean {
        return other is LeftBracket
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun asStr() = "("
}
class RightBracket:Token(){
    override fun equals(other: Any?): Boolean {
        return other is RightBracket
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun asStr() = ")"
}
open class NameToken(val name:String):Token(){
    override fun equals(other:Any?):Boolean{
        return other is NameToken && other.name == this.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun asStr() = name
}
open class NumberToken(val number:Double):Token(){
    override fun equals(other: Any?): Boolean {
        return other is NumberToken && other.number == this.number
    }

    override fun hashCode(): Int {
        return number.hashCode()
    }

    override fun asStr() = if (round(number) == number) number.toInt().toString() else number.toString()
}

class Tokenizer{
    val tokensStr:List<String>
    val tokens:List<Token>
    var current = 0
    var marks:ArrayDeque<Int> = ArrayDeque()
    constructor(expression:String) {
        val tokens1 = Split(reserved_symbs,expression)
        var result = mutableListOf<String>()
        for (token in tokens1){
            result.addAll(Extract_kx(token))
        }
        tokensStr = result.toList()
        tokens = tokensStr.map{it -> StrToToken(it)}
    }
    constructor(tokens:List<Token>){
        this.tokens = tokens
        this.tokensStr = listOf()
    }
    constructor(vararg tokens:Token):this(tokens.toList())
    fun GetToken(to_next:Boolean = false):Token{
        var result = tokens[current]
        if (to_next) current++
        return result
    }
    fun Skip(){
        current++
    }
    fun Mark(){
        marks.addLast(current)
    }
    fun Reset(){
        current = marks.removeLast()
    }
    fun Reset(pos:Int){
        current = pos
    }
    operator fun get(pos:Int):Token{
        return tokens[pos]
    }
    fun isEnd():Boolean{
        return current > tokens.lastIndex
    }
    fun isEmpty():Boolean{
        return tokens.isEmpty()
    }
    fun Size():Int{
        return tokens.size
    }
    override fun equals(other: Any?): Boolean {
        return other is Tokenizer && other.tokens == tokens
    }
    companion object {
        fun StrToToken(str:String):Token {
            if (str == "("){
                return LeftBracket()
            }
            if (str == ")"){
                return RightBracket()
            }
            if (CheckName(str)) {
                return NameToken(str)
            }
            if (CheckNumber(str)){
                return NumberToken(str.toDouble())
            }

            throw IncorrectTokenException("incorrect token ${str} does not match to any type of tokens")
        }
        private fun CheckName(s: String): Boolean {
            return s[0].isLetter() && s.all { it -> it.isLetterOrDigit() || it == '_'} || reserved_symbs_set.contains(s)
        }

        private fun CheckNumber(s: String): Boolean {
            try {
                if (s.last()=='.') {
                    return false
                }
                var v = s.toDouble()
                return true
            } catch (e: Exception) {
                return false
            }
        }

        private fun Extract_kx(s: String): List<String> {
            if (s.matches(Regex("[\\d]+(\\.[\\d]+)?[^\\d_.]+.*"))) {
                var destructured = Regex("([\\d]+(\\.[\\d]+)?)([^\\d_.]+.*)").find(s)!!.destructured
                return listOf(destructured.component1(), "*", destructured.component3())
            } else {
                return listOf(s)
            }
        }
    }
}

class Tokens(tokenizer:Tokenizer){
    init {
        while (!tokenizer.isEnd()){

        }
    }
}

