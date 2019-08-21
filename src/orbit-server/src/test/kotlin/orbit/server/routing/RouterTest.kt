package orbit.server.routing

import orbit.server.*
import orbit.server.local.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class RouterTest {

    @Test
    fun `can route message to client node`() {

        val nodeDirectory = InMemoryNodeDirectory()
        val addressDirectory = InMemoryAddressableDirectory()

        val accountAddress = TestAddress()
        val message = Message(
            "This is a test message",
            accountAddress
        )
        val router = Router(NodeId("node1"), addressDirectory, nodeDirectory, TestAddressablePlacementStrategy())

        nodeDirectory.connectNode(TestRemoteNode(NodeId("node1")))
        nodeDirectory.connectNode(TestRemoteNode(NodeId("node2")));
        nodeDirectory.connectNode(LocalClientNode<TestAddress>(NodeId("client"), listOf(Capability("test"))), NodeId("node2"))

        addressDirectory.setLocation(accountAddress, NodeId("client"))

        val route = router.routeMessage(message)

        assertThat(route?.path).containsExactly(Mesh.Instance.id, NodeId("node2"), NodeId("client"))
    }

    @Test
    fun `when addressable not in node, uses strategy to pick new node`() {
        val nodeDirectory = InMemoryNodeDirectory()
        val addressDirectory = InMemoryAddressableDirectory()

        val accountAddress = TestAddress()
        val message = Message(
            "This is a test message",
            accountAddress
        )
        val router = Router(
            NodeId("node1"), addressDirectory, nodeDirectory,
            TestAddressablePlacementStrategy(NodeId("client"))
        )
        nodeDirectory.connectNode(TestRemoteNode(NodeId("node1")))
        nodeDirectory.connectNode(TestRemoteNode(NodeId("node2")));
        nodeDirectory.connectNode(LocalClientNode<TestAddress>(NodeId("client"), listOf(Capability("test"))), NodeId("node2"))

        val route = router.routeMessage(message)

        assertThat(route?.path).containsExactly(Mesh.Instance.id, NodeId("node2"), NodeId("client"))
    }

    @Test
    fun `when valid projected route is supplied, prefers that route`() {
        val nodeDirectory = InMemoryNodeDirectory()
        val addressDirectory = InMemoryAddressableDirectory()

        val accountAddress = TestAddress()
        val message = Message(
            "This is a test message",
            accountAddress
        )
        val router = Router(NodeId("node1"), addressDirectory, nodeDirectory, TestAddressablePlacementStrategy())

        nodeDirectory.connectNode(TestRemoteNode(NodeId("node1")))
        nodeDirectory.connectNode(TestRemoteNode(NodeId("node2")));
        nodeDirectory.connectNode(TestRemoteNode(NodeId("node3")));
        nodeDirectory.connectNode(LocalClientNode<TestAddress>(NodeId("client"), listOf(Capability("test"))), NodeId("node3"))

        addressDirectory.setLocation(accountAddress, NodeId("client"))

        val projectedRoute = Route(listOf(NodeId("node1"), NodeId("mesh"), NodeId("node2"), NodeId("mesh"), NodeId("node3"), NodeId("client")))
        val route = router.routeMessage(message, projectedRoute)

        assertThat(route?.path).containsExactlyElementsOf(projectedRoute.pop().route.path)
    }

    @Test
    fun `when invalid projected route is supplied, finds new route`() {
        val nodeDirectory = InMemoryNodeDirectory()
        val addressDirectory = InMemoryAddressableDirectory()

        val accountAddress = TestAddress()
        val message = Message(
            "This is a test message",
            accountAddress
        )
        val router = Router(NodeId("node1"), addressDirectory, nodeDirectory, TestAddressablePlacementStrategy())

        nodeDirectory.connectNode(TestRemoteNode(NodeId("node1")))
        nodeDirectory.connectNode(TestRemoteNode(NodeId("node2")));
        nodeDirectory.connectNode(TestRemoteNode(NodeId("node3")));
        nodeDirectory.connectNode(LocalClientNode<TestAddress>(NodeId("client"), listOf(Capability("test"))), NodeId("node3"))

        addressDirectory.setLocation(accountAddress, NodeId("client"))

        val route = router.routeMessage(message, Route(listOf(NodeId("node1"), NodeId("client"))))

        assertThat(route?.path).containsExactly(Mesh.Instance.id, NodeId("node3"), NodeId("client"))

    }

    class TestAddress : BaseAddress() {
        override fun capability(): Capability {
            return Capability("test")
        }
    }

    class TestAddressablePlacementStrategy(val selectedNode: NodeId = NodeId("")) : AddressablePlacementStrategy {
        override fun chooseNode(address: BaseAddress): NodeId {
            return selectedNode
        }
    }

    class TestRemoteNode(override val id: NodeId) : MeshNode {
        override fun <T : BaseAddress> canHandle(address: T): Boolean {
            return true
        }

        override fun sendMessage(message: BaseMessage, route: Route) {
            println("Sending message on Node ${id}: ${message.content}")
        }

        override val capabilities = listOf(Capability.Routing)
    }
}