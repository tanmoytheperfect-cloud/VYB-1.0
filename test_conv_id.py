with open("app/src/main/java/com/example/ui/viewmodel/VybViewModel.kt") as f:
    text = f.read()
print("currentConversationId in file:", "_currentConversationId" in text)
