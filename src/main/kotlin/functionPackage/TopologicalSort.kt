package functionPackage

class Node(val id:Int){
    val next: ArrayDeque<Node> = ArrayDeque()
    var previousCount:Int = 0
    fun visit(){
        --previousCount
    }
    fun addNext(newNext:Node){
        next.add(newNext)
        ++newNext.previousCount
    }
    fun isReady():Boolean = previousCount == 0
}
class Graph(val initials:ArrayDeque<Node>){
    constructor(edges:List<Pair<Int,Int>>):this(Graph.build(edges))
    companion object {
        fun build(edges: List<Pair<Int, Int>>): ArrayDeque<Node> {
            val allNodes: HashMap<Int, Node> = hashMapOf()
            for (edge in edges) {
                val previousId = edge.first
                val nextId = edge.second
                if (!allNodes.containsKey(previousId)) {
                    allNodes[previousId] = Node(previousId)
                }
                if (!allNodes.containsKey(nextId)) {
                    allNodes[nextId] = Node(nextId)
                }
                val previousNode = allNodes[previousId]!!
                val nextNode = allNodes[nextId]!!
                previousNode.addNext(nextNode)
            }
            val initials: ArrayDeque<Node> = ArrayDeque()
            for (node in allNodes.values) {
                if (node.isReady()) {
                    initials.add(node)
                }
            }
            return initials
        }
    }
    fun topologicalSort():ArrayDeque<Int>{
        val stack = initials
        val result = ArrayDeque<Int>()
        var currentNode:Node
        while (!stack.isEmpty()){
            currentNode = stack.removeLast()
            result.add(currentNode.id)
            for (node in currentNode.next){
                node.visit()
                if (node.isReady()){
                    stack.add(node)
                }
            }
        }
        return result
    }
}

fun <T> sortTopologically(initials:ArrayDeque<T>,toNext:(T)->List<T>,toId:(T)->Int):ArrayDeque<T>{
    val visited:MutableSet<Int> = mutableSetOf()
    val idToT:HashMap<Int,T> = hashMapOf()
    val nodesT:HashMap<Int,Node> = initials.map{toId(it)}.zip(initials.map{Node(toId(it))}).toHashMap()
    val queue = ArrayDeque(initials)
    var nodeT:T
    var next:List<T>
    var id:Int
    while(!queue.isEmpty()){
        nodeT = queue.removeFirst()
        id = toId(nodeT)
        idToT[id] = nodeT
        next = toNext(nodeT)
        var localId:Int
        for (nextNodeT in next){
            localId = toId(nextNodeT)
            if (!visited.contains(localId)){
                queue.add(nextNodeT)
            }
            if (!nodesT.contains(localId)){
                nodesT[localId] = Node(localId)
            }
            nodesT[id]!!.addNext(nodesT[localId]!!)
        }
        visited.add(id)
    }
    val resultNodes = ArrayDeque(initials.map{it -> nodesT[toId(it)]!!})
    val graph = Graph(resultNodes)
    val sorting = graph.topologicalSort()
    return ArrayDeque(sorting.map{idToT[it]!!})
}
